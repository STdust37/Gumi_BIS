package service;

import java.util.ArrayList;

/**
 * 검색 서비스 공통 계약 역할
 */
public interface SearchService<T> {
    ArrayList<T> search(String keyword);
}
