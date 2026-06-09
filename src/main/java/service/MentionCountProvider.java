package service;

import java.io.IOException;

/**
 * 장소 최근 언급량 조회 계약 역할
 */
public interface MentionCountProvider {
    int countRecentBlogMentions(String placeName) throws IOException, InterruptedException;
}
