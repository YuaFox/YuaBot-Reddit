package dev.yuafox.yuabot.plugins.reddit;

import dev.yuafox.yuabot.YuaBot;
import dev.yuafox.yuabot.data.Media;
import dev.yuafox.yuabot.plugins.DataController;
import dev.yuafox.yuabot.plugins.ActionHandler;
import dev.yuafox.yuabot.plugins.Plugin;
import dev.yuafox.yuabot.plugins.reddit.api.PushShift;
import dev.yuafox.yuabot.plugins.reddit.media.RedditSource;
import dev.yuafox.yuabot.utils.Https;
import dev.yuafox.yuabot.utils.Time;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class RedditPlugin extends Plugin implements DataController {

    @Override
    public void onLoad(){
        YuaBot.registerActionHandler("reddit", this);
    }

    @Override
    public String getSourceName(){
        return "Reddit";
    }

    @ActionHandler(action="install")
    public void install() throws SQLException {
        YuaBot.installMediaSource(this, RedditSource.class);
        File folder = new File(this.getBaseFolder(), "media");
        folder.mkdirs();
    }

    @ActionHandler(action="fetch")
    public void fetch() throws InterruptedException {
        String time = YuaBot.params.getOrDefault("time", List.of("none")).get(0);
        String subreddit = YuaBot.params.get("subreddit") != null ? YuaBot.params.get("subreddit").get(0) : null;
        String tag = YuaBot.params.get("tag") != null ? YuaBot.params.get("tag").get(0) : null;

        if(subreddit == null) return;

        String tend, tstart;
        switch (time) {
            case "all" -> {
                tend = Time.getLastMonth(null);
                tstart = Time.getLastMonth(tend);
                int tries = 0;
                while (tries < 2) {
                    boolean success = this.fetch(subreddit, tag, tstart, tend);
                    if(success) tries = 0;
                    else        tries++;
                    tend = tstart;
                    tstart = Time.getLastMonth(tend);
                    Thread.sleep(5000);
                }
            }
            case "lastmonth" -> {
                tend = Time.getLastMonth(null);
                tstart = Time.getLastMonth(tend);
                this.fetch(subreddit, tag, tstart, tend);
            }
            default -> System.err.println("No mode selected.");
        }
    }

    private boolean fetch(String subreddit, String tag, String start, String end) throws InterruptedException {
        YuaBot.LOGGER.info("Fetching... {} -> {}", start, end);
        List<RedditSource> posts = PushShift.get(subreddit, start, end, tag);
        if(posts == null){
            YuaBot.LOGGER.info("No posts fetched");
            return false;
        }
        YuaBot.LOGGER.info("Posts fetched: {}", posts.size());

        File folder = new File(this.getBaseFolder(), "media");

        for (RedditSource post: posts) {
            YuaBot.LOGGER.info("Post: {}", post);
            if(post == null) continue;
            try {
                int sourceId = YuaBot.createSource(this, post);

                try {
                    for (String url : post.onlineUrls) {
                        String urlLocal = folder.getPath() + "/" + new URL(url).getFile().replace("/", ".").substring(1);
                        Media media = new Media();
                        media.media = Https.download(url, urlLocal);

                        if(media.media.length() > 0L) {
                            YuaBot.createMedia(sourceId, media);
                        }
                    }
                } catch (Exception e) {
                    YuaBot.LOGGER.error("Error creating media", e);
                }
                Thread.sleep(5000);
            }catch (Exception e){
                YuaBot.LOGGER.error("Error creating source", e);
            }
        }
        YuaBot.LOGGER.info("OK.");
        return true;
    }
}
