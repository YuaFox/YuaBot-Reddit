package dev.yuafox.yuabot.plugins.reddit.media;

import dev.yuafox.yuabot.data.Data;
import dev.yuafox.yuabot.data.Source;

import java.util.List;

public class RedditSource extends Source {

    @Data(id="permalink", unique = true)
    public String permalink;

    @Data(id="subreddit", unique = false)
    public String subreddit;

    @Data(id="title", unique = false)
    public String title;

    @Data(id="tag", unique = false)
    public String tag;

    public List<String> onlineUrls;

    public RedditSource(String permalink, String subreddit, String author, String title, String flair, List<String> onlineUrls) {
        this.permalink = permalink;
        this.subreddit = subreddit;
        this.onlineUrls = onlineUrls;
        this.author = author;
        this.title = title;
        this.tag = flair;
    }
}
