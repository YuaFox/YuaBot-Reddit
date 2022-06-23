package dev.yuafox.yuabot.plugins.reddit.api;

import dev.yuafox.yuabot.YuaBot;
import dev.yuafox.yuabot.plugins.reddit.media.RedditSource;
import dev.yuafox.yuabot.utils.Https;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PushShift {
    public static List<RedditSource> get(String subreddit, String start, String end, String tag) {
        try {
            List<RedditSource> redditPosts = new ArrayList<>();

            JSONObject data = Https.getJsonObject("https://api.pushshift.io/reddit/search/submission/?subreddit="+subreddit+"&sort=desc&sort_type=created_utc&before="+end+"&after="+start+"&size=1000");
            JSONArray posts = data.getJSONArray("data");
            if(posts.length() == 0) return null;
            for(int i = 0; i < posts.length(); i++){
                JSONObject post = posts.getJSONObject(i);

                try {
                    String id = post.getString("id");
                    String name = post.has("author_fullname") ?
                            post.getString("author_fullname") : null;
                    String title = post.has("title") ?
                            post.getString("title") : "";
                    String flair = post.has("link_flair_text") ?
                            post.getString("link_flair_text").trim() : null;
                    String urlDest = post.has("url_overridden_by_dest") ?
                            post.getString("url_overridden_by_dest") : null;
                    String domain = post.has("domain") ?
                            post.getString("domain") : null;

                    // TODO: Detect crossposting properly
                    boolean isGallery = post.has("is_gallery") && post.getBoolean("is_gallery");
                    boolean isVideo = post.has("is_video") && post.getBoolean("is_video") || urlDest != null && urlDest.contains("v.redd.it");
                    boolean isRemoved = post.has("removed_by_category");
                    boolean isReddit = "v.redd.it".equals(domain) ||"i.redd.it".equals(domain) || "reddit.com".equals(domain) || (domain != null && domain.contains("/r/"));

                    RedditSource redditPost = null;

                    if (isReddit && !isRemoved && urlDest != null) {
                        if (isVideo) {
                            redditPost = new RedditSource(id, subreddit, name, title, flair, List.of(urlDest + "/DASH_480.mp4"));
                        } else if (isGallery) {
                            List<String> urlList = new ArrayList<>();
                            JSONArray galleryData = post.getJSONObject("gallery_data").getJSONArray("items");
                            JSONObject mediaMetadata = post.getJSONObject("media_metadata");

                            for (int j = 0; j < galleryData.length(); j++) {
                                String mediaId = galleryData.getJSONObject(j).getString("media_id");
                                String mediaType = mediaMetadata.getJSONObject(mediaId).getString("m").replace("image/", "");
                                urlList.add("https://i.redd.it/" + mediaId + "." + mediaType);
                            }

                            redditPost = new RedditSource(id, subreddit, name, title, flair, urlList);
                        } else {
                            redditPost = new RedditSource(id, subreddit, name, title, flair, List.of(urlDest));
                        }
                    }

                    if (tag == null || redditPost != null && Objects.equals(tag, redditPost.tag))
                        redditPosts.add(redditPost);
                }catch (Exception e){
                    YuaBot.LOGGER.error("Error fetching post.", e);
                    YuaBot.LOGGER.error("Post: {}", post);
                }
            }

            return redditPosts;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
