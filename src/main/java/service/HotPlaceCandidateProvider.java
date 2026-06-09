package service;

import model.HotPlace;

import java.io.IOException;
import java.util.ArrayList;

/**
 * 핫플레이스 후보 제공 계약 역할
 */
public interface HotPlaceCandidateProvider {
    ArrayList<HotPlace> searchHotPlaceCandidates() throws IOException, InterruptedException;
}
