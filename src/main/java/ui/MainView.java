package ui;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.ArrivalInfo;
import model.BusLocation;
import model.BusStop;
import model.HotPlace;
import model.HotPlaceTransitOption;
import model.HotPlaceTransitPlan;
import model.NearbyStopCandidate;
import model.Route;
import model.RouteStop;
import client.GooglePlacesClient;
import client.NaverSearchHotPlaceClient;
import service.BisSearchService;
import service.FavoriteStore;
import service.HotPlaceService;
import service.HotPlaceTransitRecommender;
import service.RecentSearchStore;
import ui.component.ArrivalCard;
import ui.component.FavoriteCell;
import ui.component.HotPlaceCell;
import ui.component.RouteStopCell;
import ui.component.SearchResultCell;
import ui.component.TransitOptionCell;
import ui.viewmodel.SearchResult;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class MainView implements ViewController {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ROUTE_SCROLL_CONTEXT_STOPS = 2;

    private final BisSearchService service;
    private final HotPlaceTransitRecommender hotPlaceTransitRecommender;
    private final HotPlaceService hotPlaceService = new HotPlaceService(new NaverSearchHotPlaceClient(), new GooglePlacesClient());
    private final FavoriteStore favoriteStore = new FavoriteStore();
    private final RecentSearchStore recentSearchStore = new RecentSearchStore();

    private final BorderPane root = new BorderPane();
    private final TextField searchField = new TextField();
    private final TextField destinationField = new TextField();
    private final Button searchButton = new Button("🔍");
    private final VBox inlineRecentSearchBox = new VBox(4);
    private final Label searchCountLabel = new Label("");
    private final Label noSearchResultLabel = new Label("검색 결과가 없습니다. 다시 시도해주세요");
    private final ProgressIndicator searchLoadingIndicator = new ProgressIndicator();
    private final ProgressIndicator busyIndicator = new ProgressIndicator();
    private final Label statusLabel = new Label("시작 중");
    private final Label summaryLabel = new Label("데이터 로딩 대기");
    private final VBox searchResultTabs = new VBox(0);
    private final HBox searchResultTabBar = new HBox(0);
    private final Button routeResultTab = new Button("버스(0)");
    private final Button stopResultTab = new Button("정류장(0)");
    private final ListView<SearchResult> routeResultList = new ListView<>();
    private final ListView<SearchResult> stopResultList = new ListView<>();
    private final ListView<RouteStop> routeStopList = new ListView<>();
    private final ListView<ArrivalInfo> arrivalList = new ListView<>();
    private final ListView<BusLocation> busLocationList = new ListView<>();
    private final ListView<HotPlace> hotPlaceList = new ListView<>();
    private final HBox hotPlaceTabBar = new HBox(0);
    private final Button hotPlaceAllTab = new Button("전체(0)");
    private final Button hotPlaceAttractionTab = new Button("명소(0)");
    private final Button hotPlaceFoodTab = new Button("먹거리(0)");
    private final ListView<HotPlaceTransitOption> hotPlaceTransitList = new ListView<>();
    private final ListView<String> favoriteList = new ListView<>();
    private final Label detailTitle = new Label("선택된 항목 없음");
    private final Label detailSubTitle = new Label("검색 결과에서 정류장 또는 노선을 선택하세요.");
    private final Label contentTitle = new Label("정보");
    private final Label routeMetaLabel = new Label("");
    private final Label updatedAtLabel = new Label("");
    private final VBox detailContent = new VBox(10);
    private final HBox breadcrumbBox = new HBox(6);
    private final ComboBox<String> arrivalSortBox = new ComboBox<>();
    private final Button destinationFilterButton = new Button("필터");
    private final Button clearDestinationFilterButton = new Button("x");
    private final Button backButton = new Button("← 뒤로");
    private final Button favoriteButton = new Button("☆");
    private final Button detailRefreshButton = new Button("↻");
    private final Button hotPlaceRefreshButton = new Button("↻");
    private final Label hotPlaceStatusLabel = new Label("");
    private final TextField hotPlaceDepartureField = new TextField();
    private final Button hotPlaceRecommendButton = new Button("교통편 추천");
    private final Label hotPlaceDestinationLabel = new Label("");
    private final Label hotPlaceTransitStatusLabel = new Label("");
    private final Button refreshButton = new Button("전체 새로고침");
    private final Button clearButton = new Button("초기화");

    private BusStop selectedStop;
    private Route selectedRoute;
    private SearchResult selectedRouteResult;
    private SearchResult selectedStopResult;
    private HotPlace selectedHotPlace;
    private NearbyStopCandidate selectedHotPlaceDestination;
    private String selectedFavoriteRow;
    private String highlightedStopId;
    private String currentFavoriteType;
    private String currentFavoriteId;
    private String currentFavoriteLabel;
    private String currentRouteId;
    private String currentRouteName;
    private String highlightedArrivalRouteId;
    private String activeDestinationFilter = "";
    private ArrayList<BusLocation> currentBusLocations = new ArrayList<>();
    private HashMap<Integer, ArrayList<BusLocation>> currentBusLocationsByStopSeq = new HashMap<>();
    private ArrayList<ArrivalInfo> currentArrivals = new ArrayList<>();
    private ArrayList<ArrivalInfo> filteredArrivals = new ArrayList<>();
    private HotPlaceService.HotPlaceResult currentHotPlaceResult = HotPlaceService.HotPlaceResult.empty();
    private String activeHotPlaceTab = "ALL";
    private final ArrayList<NavigationItem> navigationItems = new ArrayList<>();

    public MainView(BisSearchService service) {
        this.service = service;
        this.hotPlaceTransitRecommender = new HotPlaceTransitRecommender(service);
        buildLayout();
        bindActions();
    }

    public Parent getRoot() {
        return root;
    }

    public void loadInitialData() {
        loadHotPlaces();
        runBackground("전체 노선/정류장 데이터를 불러오는 중입니다.", () -> {
            service.loadMasterData();
            return null;
        }, ignored -> {
            summaryLabel.setText("노선 " + service.getRoutes().size() + "개, 정류장 " + service.getStops().size() + "개 로드됨");
            statusLabel.setText("검색할 수 있습니다.");
            performSearch();
        });
    }

    private void buildLayout() {
        root.getStyleClass().add("app-root");
        root.setTop(buildHeader());
        root.setLeft(buildSearchPane());
        root.setCenter(buildDetailPane());
        root.setRight(buildSidePane());
        BorderPane.setMargin(root.getLeft(), new Insets(0, 0, 0, 0));
    }

    private Parent buildHeader() {
        Label appTitle = new Label("Gumi BIS");
        appTitle.getStyleClass().add("app-title");

        Label appSubTitle = new Label("실시간 버스 정보");
        appSubTitle.getStyleClass().add("app-subtitle");

        VBox titleBox = new VBox(2, appTitle, appSubTitle);
        HBox controls = new HBox(8, refreshButton, clearButton);
        controls.setAlignment(Pos.CENTER_RIGHT);

        HBox header = new HBox(16, titleBox, spacer(), summaryLabel, controls);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private Parent buildSearchPane() {
        searchField.setPromptText("정류장 이름/ID 또는 버스 번호");
        searchButton.setDefaultButton(true);
        searchButton.getStyleClass().add("search-icon-button");
        searchCountLabel.getStyleClass().add("search-count-label");
        searchCountLabel.setManaged(false);
        searchCountLabel.setVisible(false);
        noSearchResultLabel.getStyleClass().add("no-search-result-label");
        noSearchResultLabel.setManaged(false);
        noSearchResultLabel.setVisible(false);
        searchLoadingIndicator.getStyleClass().add("search-loading-indicator");
        searchLoadingIndicator.setManaged(false);
        searchLoadingIndicator.setVisible(false);
        searchLoadingIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        inlineRecentSearchBox.getStyleClass().add("inline-recent-box");
        inlineRecentSearchBox.setManaged(false);
        inlineRecentSearchBox.setVisible(false);

        HBox searchRow = new HBox(6, searchField, searchLoadingIndicator, searchButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        configureSearchResultList(routeResultList, true);
        configureSearchResultList(stopResultList, false);
        routeResultTab.getStyleClass().add("search-tab-button");
        stopResultTab.getStyleClass().add("search-tab-button");
        routeResultTab.setMaxWidth(Double.MAX_VALUE);
        stopResultTab.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(routeResultTab, Priority.ALWAYS);
        HBox.setHgrow(stopResultTab, Priority.ALWAYS);
        searchResultTabBar.getChildren().setAll(routeResultTab, stopResultTab);
        searchResultTabBar.getStyleClass().add("search-result-tab-bar");
        searchResultTabs.getStyleClass().add("search-result-tabs");
        selectSearchResultTab(true);

        searchButton.setOnAction(event -> performSearch());
        searchField.setOnAction(event -> performSearch());
        routeResultTab.setOnAction(event -> selectSearchResultTab(true));
        stopResultTab.setOnAction(event -> selectSearchResultTab(false));

        VBox pane = new VBox(10,
                new Label("검색"),
                searchRow,
                inlineRecentSearchBox,
                searchCountLabel,
                noSearchResultLabel,
                searchResultTabs);
        pane.getStyleClass().add("search-pane");
        VBox.setVgrow(searchResultTabs, Priority.ALWAYS);
        return pane;
    }

    private void selectSearchResultTab(boolean routeTab) {
        routeResultTab.getStyleClass().remove("active-search-tab");
        stopResultTab.getStyleClass().remove("active-search-tab");
        if (routeTab) {
            routeResultTab.getStyleClass().add("active-search-tab");
            searchResultTabs.getChildren().setAll(searchResultTabBar, routeResultList);
            VBox.setVgrow(routeResultList, Priority.ALWAYS);
        } else {
            stopResultTab.getStyleClass().add("active-search-tab");
            searchResultTabs.getChildren().setAll(searchResultTabBar, stopResultList);
            VBox.setVgrow(stopResultList, Priority.ALWAYS);
        }
    }

    private void configureSearchResultList(ListView<SearchResult> listView, boolean routeList) {
        listView.setPlaceholder(new Label(""));
        listView.setCellFactory(currentList -> new SearchResultCell(
                () -> searchField.getText(),
                () -> routeList ? selectedRouteResult : selectedStopResult,
                item -> selectSearchResult(listView, item, routeList),
                this::handleSearchResult));
    }

    private Parent buildDetailPane() {
        detailTitle.getStyleClass().add("detail-title");
        detailSubTitle.getStyleClass().add("detail-subtitle");
        contentTitle.getStyleClass().add("section-title");
        statusLabel.getStyleClass().add("detail-status-label");
        busyIndicator.getStyleClass().add("busy-indicator");
        busyIndicator.setManaged(false);
        busyIndicator.setVisible(false);
        busyIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        updatedAtLabel.getStyleClass().add("updated-label");
        breadcrumbBox.getStyleClass().add("breadcrumb-box");
        backButton.getStyleClass().add("back-button");
        backButton.setDisable(true);

        favoriteButton.getStyleClass().add("favorite-button");
        detailRefreshButton.getStyleClass().add("icon-button");
        detailRefreshButton.setDisable(true);

        routeStopList.setPlaceholder(new Label("노선을 선택하면 정류장 순서가 표시됩니다."));
        routeStopList.setCellFactory(listView -> new RouteStopCell(() -> highlightedStopId, () -> currentBusLocationsByStopSeq));

        arrivalList.setPlaceholder(new Label("정류장을 선택하면 도착 정보가 표시됩니다."));
        arrivalList.setCellFactory(listView -> new ArrivalCard(
                this::isArrivalFavorite,
                this::toggleArrivalFavorite,
                () -> highlightedArrivalRouteId));

        arrivalSortBox.setItems(FXCollections.observableArrayList("도착순", "번호순"));
        arrivalSortBox.setValue("도착순");
        arrivalSortBox.getStyleClass().add("sort-box");
        destinationField.setPromptText("도착지 정류장 이름/ID");
        destinationFilterButton.getStyleClass().add("filter-button");
        clearDestinationFilterButton.getStyleClass().add("filter-clear-button");

        busLocationList.setPlaceholder(new Label("운행 중인 버스 위치가 없습니다."));
        busLocationList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(BusLocation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText("차량 " + value(item.getBidNo()) + " / " + value(item.getStopKname())
                        + " / " + value(item.getRemainStop()) + "정류장 전");
            }
        });

        hotPlaceDepartureField.setPromptText("출발 정류장 이름/ID");
        hotPlaceRecommendButton.getStyleClass().add("filter-button");
        hotPlaceDestinationLabel.getStyleClass().add("hot-place-destination-label");
        hotPlaceDestinationLabel.setWrapText(true);
        hotPlaceTransitStatusLabel.getStyleClass().add("detail-status-label");
        hotPlaceTransitList.setPlaceholder(new Label("출발 정류장을 입력하면 추천 노선이 표시됩니다."));
        hotPlaceTransitList.setCellFactory(listView -> new TransitOptionCell());

        detailContent.getStyleClass().add("detail-content");
        Label emptyText = new Label("검색 결과에서 항목을 선택하면 정보가 표시됩니다.");
        emptyText.getStyleClass().add("empty-detail");
        detailContent.getChildren().setAll(emptyText);

        HBox actionBar = new HBox(8, favoriteButton, detailRefreshButton, updatedAtLabel, routeMetaLabel);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        HBox navigationBar = new HBox(8, backButton, breadcrumbBox);
        navigationBar.getStyleClass().add("navigation-bar");
        navigationBar.setAlignment(Pos.CENTER_LEFT);

        HBox statusRow = new HBox(6, busyIndicator, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox pane = new VBox(10, navigationBar, detailTitle, detailSubTitle, actionBar, statusRow, contentTitle, detailContent);
        pane.getStyleClass().add("detail-pane");
        VBox.setVgrow(detailContent, Priority.ALWAYS);
        return pane;
    }

    private Parent buildSidePane() {
        hotPlaceList.getStyleClass().add("hot-place-list");
        hotPlaceList.setPlaceholder(new Label("핫플 목록을 불러오는 중입니다."));
        hotPlaceList.setCellFactory(listView -> new HotPlaceCell());
        hotPlaceRefreshButton.getStyleClass().add("side-refresh-button");
        hotPlaceStatusLabel.getStyleClass().add("side-subtitle");
        configureHotPlaceTabButton(hotPlaceAllTab, "ALL");
        configureHotPlaceTabButton(hotPlaceAttractionTab, "ATTRACTION");
        configureHotPlaceTabButton(hotPlaceFoodTab, "FOOD");
        hotPlaceTabBar.getChildren().setAll(hotPlaceAllTab, hotPlaceAttractionTab, hotPlaceFoodTab);
        hotPlaceTabBar.getStyleClass().add("hot-place-tab-bar");

        Label hotTitle = new Label("구미 핫플");
        hotTitle.getStyleClass().add("side-title");
        HBox hotHeader = new HBox(8, hotTitle, spacer(), hotPlaceRefreshButton);
        hotHeader.setAlignment(Pos.CENTER_LEFT);
        VBox hotSection = new VBox(6, hotHeader, hotPlaceStatusLabel, hotPlaceTabBar, hotPlaceList);
        hotSection.getStyleClass().add("side-section");
        VBox.setVgrow(hotPlaceList, Priority.ALWAYS);

        favoriteList.setPlaceholder(new Label("즐겨찾기가 없습니다."));
        favoriteList.setCellFactory(listView -> new FavoriteCell(
                this::removeFavorite,
                () -> selectedFavoriteRow,
                this::selectFavorite,
                this::openFavorite));

        Label title = new Label("즐겨찾기");
        title.getStyleClass().add("side-title");
        Label subtitle = new Label("자주 보는 정류장과 노선");
        subtitle.getStyleClass().add("side-subtitle");

        VBox favoriteSection = new VBox(6, title, subtitle, favoriteList);
        favoriteSection.getStyleClass().add("side-section");
        VBox.setVgrow(favoriteList, Priority.ALWAYS);

        VBox pane = new VBox(12, hotSection, favoriteSection);
        pane.getStyleClass().add("side-pane");
        VBox.setVgrow(hotSection, Priority.ALWAYS);
        VBox.setVgrow(favoriteSection, Priority.ALWAYS);
        return pane;
    }

    private void configureHotPlaceTabButton(Button button, String tabId) {
        button.getStyleClass().add("hot-place-tab-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> selectHotPlaceTab(tabId));
        HBox.setHgrow(button, Priority.ALWAYS);
    }

    private void refreshHotPlaceTabs() {
        hotPlaceAllTab.setText("전체(" + currentHotPlaceResult.getAllPlaces().size() + ")");
        hotPlaceAttractionTab.setText("명소(" + currentHotPlaceResult.getAttractionPlaces().size() + ")");
        hotPlaceFoodTab.setText("먹거리(" + currentHotPlaceResult.getFoodPlaces().size() + ")");
        if ("ATTRACTION".equals(activeHotPlaceTab) && currentHotPlaceResult.getAttractionPlaces().isEmpty()) {
            activeHotPlaceTab = "ALL";
        }
        if ("FOOD".equals(activeHotPlaceTab) && currentHotPlaceResult.getFoodPlaces().isEmpty()) {
            activeHotPlaceTab = "ALL";
        }
        selectHotPlaceTab(activeHotPlaceTab);
    }

    private void selectHotPlaceTab(String tabId) {
        activeHotPlaceTab = tabId;
        hotPlaceAllTab.getStyleClass().remove("active-hot-place-tab");
        hotPlaceAttractionTab.getStyleClass().remove("active-hot-place-tab");
        hotPlaceFoodTab.getStyleClass().remove("active-hot-place-tab");

        if ("ATTRACTION".equals(tabId)) {
            hotPlaceAttractionTab.getStyleClass().add("active-hot-place-tab");
            hotPlaceList.setItems(FXCollections.observableArrayList(currentHotPlaceResult.getAttractionPlaces()));
        } else if ("FOOD".equals(tabId)) {
            hotPlaceFoodTab.getStyleClass().add("active-hot-place-tab");
            hotPlaceList.setItems(FXCollections.observableArrayList(currentHotPlaceResult.getFoodPlaces()));
        } else {
            hotPlaceAllTab.getStyleClass().add("active-hot-place-tab");
            hotPlaceList.setItems(FXCollections.observableArrayList(currentHotPlaceResult.getAllPlaces()));
        }
    }

    private void bindActions() {
        routeStopList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                RouteStop selected = routeStopList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    selectedStop = new BusStop(selected.getServiceId(), selected.getStopName(), selected.getStopX(), selected.getStopY());
                    highlightedStopId = selected.getServiceId();
                    loadArrivalsForStop(selectedStop);
                }
            }
        });

        arrivalList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ArrivalInfo selected = arrivalList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadRouteDetail(selected.getRouteId(), selected.getBrtId(), highlightedStopId);
                }
            }
        });

        hotPlaceList.setOnMouseClicked(event -> {
            HotPlace selected = hotPlaceList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openHotPlaceTransit(selected);
            }
        });

        hotPlaceTransitList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                HotPlaceTransitOption selected = hotPlaceTransitList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    loadArrivalsForStop(selected.getDepartureStop(), true, true, selected.getRouteId());
                }
            }
        });

        favoriteButton.setOnAction(event -> toggleFavorite());
        detailRefreshButton.setOnAction(event -> refreshCurrentDetail());
        backButton.setOnAction(event -> goBack());
        destinationField.setOnAction(event -> applyDestinationFilter());
        destinationFilterButton.setOnAction(event -> applyDestinationFilter());
        clearDestinationFilterButton.setOnAction(event -> clearDestinationFilter());
        arrivalSortBox.setOnAction(event -> applyArrivalSort());
        hotPlaceDepartureField.setOnAction(event -> recommendHotPlaceTransit());
        hotPlaceRecommendButton.setOnAction(event -> recommendHotPlaceTransit());
        hotPlaceRefreshButton.setOnAction(event -> loadHotPlaces());
        refreshButton.setOnAction(event -> loadInitialData());
        clearButton.setOnAction(event -> clearSelection());
    }

    private void loadHotPlaces() {
        hotPlaceStatusLabel.setText("REST 조회 중");
        hotPlaceRefreshButton.setDisable(true);
        currentHotPlaceResult = HotPlaceService.HotPlaceResult.empty();
        refreshHotPlaceTabs();

        Task<HotPlaceService.HotPlaceResult> task = new Task<>() {
            @Override
            protected HotPlaceService.HotPlaceResult call() throws Exception {
                return hotPlaceService.loadHotPlaceResult();
            }
        };

        task.setOnSucceeded(event -> {
            currentHotPlaceResult = task.getValue();
            refreshHotPlaceTabs();
            hotPlaceStatusLabel.setText(hotPlaceService.getLastLoadMessage());
            hotPlaceRefreshButton.setDisable(false);
        });
        task.setOnFailed(event -> {
            currentHotPlaceResult = HotPlaceService.HotPlaceResult.empty();
            refreshHotPlaceTabs();
            Throwable error = task.getException();
            hotPlaceStatusLabel.setText(error == null ? "조회 실패" : compactHotPlaceError(error.getMessage()));
            hotPlaceRefreshButton.setDisable(false);
        });

        Thread thread = new Thread(task, "hot-place-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private String compactHotPlaceError(String message) {
        if (message == null || message.isBlank()) {
            return "조회 실패";
        }
        if (message.contains("NAVER_CLIENT_ID")) {
            return "네이버 API 키 필요";
        }
        return message;
    }

    private void openHotPlaceTransit(HotPlace hotPlace) {
        openHotPlaceTransit(hotPlace, true);
    }

    private void openHotPlaceTransit(HotPlace hotPlace, boolean recordNavigation) {
        if (!service.isMasterDataLoaded()) {
            statusLabel.setText("노선/정류장 데이터 로딩 후 핫플 교통편을 추천할 수 있습니다.");
            return;
        }
        if (recordNavigation) {
            startNavigationItem(NavigationItem.hotPlace(hotPlace));
        }

        selectedHotPlace = hotPlace;
        selectedHotPlaceDestination = hotPlaceTransitRecommender.resolvePrimaryDestination(hotPlace);
        selectedStop = null;
        selectedRoute = null;
        highlightedStopId = selectedHotPlaceDestination == null
                ? null
                : selectedHotPlaceDestination.getStop().getStopServiceid();
        currentFavoriteType = null;
        currentFavoriteId = null;
        currentFavoriteLabel = null;
        currentRouteId = null;
        currentRouteName = null;
        highlightedArrivalRouteId = null;
        currentBusLocations = new ArrayList<>();
        currentBusLocationsByStopSeq = new HashMap<>();
        currentArrivals = new ArrayList<>();
        filteredArrivals = new ArrayList<>();
        resetDestinationFilter();
        updateFavoriteButton();

        detailTitle.setText(hotPlace.getName());
        detailSubTitle.setText(hotPlaceSubtitle(hotPlace));
        detailSubTitle.setManaged(true);
        detailSubTitle.setVisible(true);
        contentTitle.setText("핫플 교통편 추천");
        routeMetaLabel.setText("");
        updatedAtLabel.setText("");
        detailRefreshButton.setDisable(true);
        hotPlaceDepartureField.clear();
        hotPlaceTransitList.getItems().clear();
        hotPlaceDestinationLabel.setText(buildHotPlaceDestinationText(hotPlace, selectedHotPlaceDestination));
        hotPlaceTransitStatusLabel.setText(selectedHotPlaceDestination == null
                ? "핫플 좌표 또는 주변 정류장을 확인할 수 없습니다."
                : "출발 정류장을 입력하세요.");

        showHotPlaceTransitContent();
        statusLabel.setText("핫플 교통편 추천 준비됨");
    }

    private void showHotPlaceTransitContent() {
        HBox departureRow = new HBox(8, hotPlaceDepartureField, hotPlaceRecommendButton);
        departureRow.getStyleClass().add("hot-place-transit-input-row");
        departureRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(hotPlaceDepartureField, Priority.ALWAYS);

        VBox summary = new VBox(7, hotPlaceDestinationLabel, departureRow, hotPlaceTransitStatusLabel);
        summary.getStyleClass().add("hot-place-transit-summary");

        detailContent.getChildren().setAll(summary, hotPlaceTransitList);
        VBox.setVgrow(hotPlaceTransitList, Priority.ALWAYS);
    }

    private void recommendHotPlaceTransit() {
        if (selectedHotPlace == null) {
            statusLabel.setText("핫플을 먼저 선택하세요.");
            return;
        }
        if (selectedHotPlaceDestination == null) {
            statusLabel.setText("핫플 주변 도착 정류장을 찾을 수 없습니다.");
            return;
        }

        String keyword = hotPlaceDepartureField.getText() == null ? "" : hotPlaceDepartureField.getText().trim();
        if (keyword.isBlank()) {
            statusLabel.setText("출발 정류장을 입력하세요.");
            return;
        }

        BusStop departureStop = findBestStop(keyword);
        if (departureStop == null) {
            statusLabel.setText("출발 정류장을 찾을 수 없습니다: " + keyword);
            return;
        }

        runBackground("핫플 교통편을 추천하는 중입니다.", () ->
                hotPlaceTransitRecommender.recommend(selectedHotPlace, departureStop), plan -> {
            selectedHotPlaceDestination = plan.getPrimaryDestinationStop();
            hotPlaceDestinationLabel.setText(buildHotPlaceDestinationText(plan.getHotPlace(), selectedHotPlaceDestination));
            hotPlaceTransitList.setItems(FXCollections.observableArrayList(plan.getOptions()));
            if (plan.getOptions().isEmpty()) {
                hotPlaceTransitStatusLabel.setText(departureStop.getStopKname() + "에서 직행 추천 노선이 없습니다.");
            } else {
                hotPlaceTransitStatusLabel.setText(departureStop.getStopKname()
                        + "에서 직행 추천 " + plan.getOptions().size() + "개");
            }
            statusLabel.setText("핫플 교통편 추천 완료");
        });
    }

    private BusStop findBestStop(String keyword) {
        ArrayList<BusStop> candidates = service.searchStops(keyword);
        if (candidates.isEmpty()) {
            return null;
        }

        String term = normalize(keyword);
        for (BusStop stop : candidates) {
            if (normalize(stop.getStopServiceid()).equals(term)) {
                return stop;
            }
        }
        for (BusStop stop : candidates) {
            if (normalize(stop.getStopKname()).equals(term)) {
                return stop;
            }
        }
        return candidates.get(0);
    }

    private String buildHotPlaceDestinationText(HotPlace hotPlace, NearbyStopCandidate candidate) {
        if (candidate == null) {
            return "핫플 주변 정류장 후보를 찾을 수 없습니다.";
        }
        BusStop stop = candidate.getStop();
        return "핫플 주변 정류장 5개 후보를 기준으로 추천합니다.\n"
                + "가장 가까운 정류장: " + stop.getStopKname()
                + " (" + stop.getStopServiceid() + ") · ["
                + hotPlace.getName() + "]에서 "
                + candidate.getRoundedDistanceMeters() + "m";
    }

    private String hotPlaceSubtitle(HotPlace hotPlace) {
        String category = value(hotPlace.getCategory());
        String address = value(hotPlace.getAddress());
        if ("-".equals(category)) {
            return address;
        }
        if ("-".equals(address)) {
            return category;
        }
        return category + " · " + address;
    }

    private void performSearch() {
        if (!service.isMasterDataLoaded()) {
            statusLabel.setText("데이터 로딩 후 검색할 수 있습니다.");
            return;
        }

        String keyword = searchField.getText() == null ? "" : searchField.getText().trim();
        if (keyword.isBlank()) {
            setSearchLoading(false);
            clearSearchResults();
            statusLabel.setText("검색어를 입력하세요.");
            return;
        }

        recentSearchStore.add("검색:" + keyword);
        setSearchLoading(true);
        runBackground("검색하는 중입니다.", () -> {
            SearchResultData data = new SearchResultData();

            ArrayList<Route> routes = service.searchRoutes(keyword);
            for (Route route : routes) {
                data.routeResults.add(SearchResult.forRoute(route));
            }

            for (BusStop stop : service.searchStops(keyword)) {
                data.stopResults.add(SearchResult.forStop(stop));
            }

            return data;
        }, data -> {
            setSearchLoading(false);
            showSearchResults(data);
            statusLabel.setText("버스 " + data.routeResults.size() + "개, 정류장 " + data.stopResults.size() + "개 검색됨");
            refreshRecentList();
        });
    }

    private void setSearchLoading(boolean loading) {
        searchLoadingIndicator.setManaged(loading);
        searchLoadingIndicator.setVisible(loading);
        searchButton.setDisable(loading);
    }

    private void showSearchResults(SearchResultData data) {
        int total = data.routeResults.size() + data.stopResults.size();
        boolean empty = data.routeResults.isEmpty() && data.stopResults.isEmpty();
        selectedRouteResult = null;
        selectedStopResult = null;
        searchCountLabel.setText("관련 항목 " + total + "개 표시됨");
        searchCountLabel.setManaged(!empty);
        searchCountLabel.setVisible(!empty);
        noSearchResultLabel.setManaged(empty);
        noSearchResultLabel.setVisible(empty);
        routeResultTab.setText("버스(" + data.routeResults.size() + ")");
        stopResultTab.setText("정류장(" + data.stopResults.size() + ")");
        routeResultList.setItems(FXCollections.observableArrayList(data.routeResults));
        stopResultList.setItems(FXCollections.observableArrayList(data.stopResults));

        if (!data.routeResults.isEmpty() || data.stopResults.isEmpty()) {
            selectSearchResultTab(true);
        } else {
            selectSearchResultTab(false);
        }
    }

    private void clearSearchResults() {
        searchCountLabel.setText("");
        searchCountLabel.setManaged(false);
        searchCountLabel.setVisible(false);
        selectedRouteResult = null;
        selectedStopResult = null;
        noSearchResultLabel.setManaged(false);
        noSearchResultLabel.setVisible(false);
        routeResultTab.setText("버스(0)");
        stopResultTab.setText("정류장(0)");
        routeResultList.getItems().clear();
        stopResultList.getItems().clear();
        selectSearchResultTab(true);
    }

    private void selectSearchResult(ListView<SearchResult> listView, SearchResult item, boolean routeList) {
        if (routeList) {
            selectedRouteResult = item;
            selectedStopResult = null;
            stopResultList.refresh();
        } else {
            selectedStopResult = item;
            selectedRouteResult = null;
            routeResultList.refresh();
        }
        listView.getSelectionModel().clearSelection();
        listView.refresh();
    }

    private void handleSearchResult(SearchResult result) {
        if (result.isStop()) {
            selectedStop = result.getStop();
            selectedRoute = null;
            highlightedStopId = selectedStop.getStopServiceid();
            startNavigationItem(NavigationItem.stop(selectedStop));
            loadArrivalsForStop(selectedStop, false);
        } else {
            selectedRoute = result.getRoute();
            selectedStop = null;
            highlightedStopId = null;
            startNavigationItem(NavigationItem.route(selectedRoute.getRouteId(), selectedRoute.getBrtId(), null));
            loadRouteDetail(selectedRoute.getRouteId(), selectedRoute.getBrtId(), null, false);
        }
    }

    private void loadArrivalsForStop(BusStop stop) {
        loadArrivalsForStop(stop, true);
    }

    private void loadArrivalsForStop(BusStop stop, boolean recordNavigation) {
        loadArrivalsForStop(stop, recordNavigation, true);
    }

    private void loadArrivalsForStop(BusStop stop, boolean recordNavigation, boolean resetFilter) {
        loadArrivalsForStop(stop, recordNavigation, resetFilter, null);
    }

    private void loadArrivalsForStop(BusStop stop, boolean recordNavigation, boolean resetFilter, String highlightedRouteId) {
        if (recordNavigation) {
            addNavigationItem(NavigationItem.stop(stop));
        }
        selectedStop = stop;
        selectedRoute = null;
        highlightedStopId = stop.getStopServiceid();
        highlightedArrivalRouteId = highlightedRouteId;
        if (resetFilter) {
            resetDestinationFilter();
        }
        detailTitle.setText(stop.getStopKname());
        detailSubTitle.setText("정류장 ID " + stop.getStopServiceid());
        detailSubTitle.setManaged(true);
        detailSubTitle.setVisible(true);
        contentTitle.setText("도착 정보");
        routeMetaLabel.setText("");
        currentFavoriteType = "STOP";
        currentFavoriteId = stop.getStopServiceid();
        currentFavoriteLabel = stop.getStopKname();
        currentRouteId = null;
        currentRouteName = null;
        currentBusLocations = new ArrayList<>();
        currentBusLocationsByStopSeq = new HashMap<>();
        updateFavoriteButton();
        detailRefreshButton.setDisable(false);

        runBackground("정류장 도착 정보를 불러오는 중입니다.", () -> service.getArrivalInfo(stop.getStopServiceid()), arrivals -> {
            currentArrivals = new ArrayList<>(arrivals);
            showArrivalContent();
            if (hasDestinationFilter()) {
                applyDestinationFilter();
            } else {
                applyArrivalSort();
            }
            updateTimestamp();
            statusLabel.setText("도착 정보 " + arrivals.size() + "개 표시");
        });
    }

    private void loadRouteDetail(String routeId, String brtId, String highlightStopId) {
        loadRouteDetail(routeId, brtId, highlightStopId, true);
    }

    private void loadRouteDetail(String routeId, String brtId, String highlightStopId, boolean recordNavigation) {
        if (recordNavigation) {
            addNavigationItem(NavigationItem.route(routeId, brtId, highlightStopId));
        }
        highlightedArrivalRouteId = null;
        highlightedStopId = highlightStopId;
        detailTitle.setText(brtId + "번 노선");
        detailSubTitle.setText("");
        detailSubTitle.setManaged(false);
        detailSubTitle.setVisible(false);
        contentTitle.setText("노선 정보");
        currentFavoriteType = "ROUTE";
        currentFavoriteId = routeId;
        currentFavoriteLabel = brtId + "번 노선";
        currentRouteId = routeId;
        currentRouteName = brtId;
        updateFavoriteButton();
        detailRefreshButton.setDisable(false);

        runBackground("노선 상세를 불러오는 중입니다.", () -> {
            RouteDetailData data = new RouteDetailData();
            data.routeStops = service.getRouteStops(routeId);
            data.busLocations = service.getBusLocations(routeId);
            return data;
        }, data -> {
            currentBusLocations = data.busLocations;
            currentBusLocationsByStopSeq = indexBusLocationsByStopSeq(data.busLocations);
            routeStopList.setItems(FXCollections.observableArrayList(data.routeStops));
            busLocationList.setItems(FXCollections.observableArrayList(data.busLocations));
            routeMetaLabel.setText("정류장 " + data.routeStops.size() + "개 / 운행 " + data.busLocations.size() + "대");
            showRouteContent();
            scrollToHighlightedStop(data.routeStops, highlightStopId);
            updateTimestamp();
            statusLabel.setText("");
        });
    }

    private void scrollToHighlightedStop(ArrayList<RouteStop> routeStops, String highlightStopId) {
        if (highlightStopId == null || highlightStopId.isBlank()) {
            return;
        }

        for (int i = 0; i < routeStops.size(); i++) {
            if (highlightStopId.equals(routeStops.get(i).getServiceId())) {
                final int index = Math.max(0, i - ROUTE_SCROLL_CONTEXT_STOPS);
                Platform.runLater(() -> routeStopList.scrollTo(index));
                return;
            }
        }
    }

    private HashMap<Integer, ArrayList<BusLocation>> indexBusLocationsByStopSeq(ArrayList<BusLocation> busLocations) {
        HashMap<Integer, ArrayList<BusLocation>> index = new HashMap<>();
        for (BusLocation bus : busLocations) {
            int destinationSeq = parseInt(bus.getBrsSeqno());
            int remainStop = parseInt(bus.getRemainStop());
            if (destinationSeq < 0 || remainStop < 0) {
                continue;
            }

            int currentSeq = destinationSeq - remainStop;
            index.computeIfAbsent(currentSeq, ignored -> new ArrayList<>()).add(bus);
        }
        return index;
    }

    private void showArrivalContent() {
        HBox filterControls = new HBox(8, destinationField, destinationFilterButton);
        filterControls.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(destinationField, Priority.ALWAYS);

        HBox toolbar = new HBox(8, filterControls, spacer(), arrivalSortBox);
        toolbar.getStyleClass().add("arrival-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filterControls, Priority.ALWAYS);

        if (hasDestinationFilter()) {
            detailContent.getChildren().setAll(toolbar, buildDestinationFilterSummary(), arrivalList);
        } else {
            detailContent.getChildren().setAll(toolbar, arrivalList);
        }
        VBox.setVgrow(arrivalList, Priority.ALWAYS);
    }

    private HBox buildDestinationFilterSummary() {
        String start = selectedStop == null ? "현재 정류장" : selectedStop.getStopKname();
        Label label = new Label(start + " -> " + activeDestinationFilter + " 가는 버스");
        label.getStyleClass().add("active-filter-label");

        HBox box = new HBox(6, label, clearDestinationFilterButton);
        box.getStyleClass().add("active-filter-box");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void applyArrivalSort() {
        ArrayList<ArrivalInfo> sorted = new ArrayList<>(visibleArrivals());
        if ("번호순".equals(arrivalSortBox.getValue())) {
            sorted.sort(Comparator.comparing(ArrivalInfo::getBrtId, this::compareRouteNumbers));
        } else {
            sorted.sort(Comparator
                    .comparingInt(this::arrivalSortValue)
                    .thenComparing(ArrivalInfo::getBrtId, this::compareRouteNumbers));
        }
        arrivalList.setItems(FXCollections.observableArrayList(sorted));
    }

    private ArrayList<ArrivalInfo> visibleArrivals() {
        if (hasDestinationFilter()) {
            return filteredArrivals;
        }
        return currentArrivals;
    }

    private void applyDestinationFilter() {
        if (selectedStop == null) {
            statusLabel.setText("정류장을 먼저 선택하세요.");
            return;
        }

        String keyword = destinationField.getText() == null ? "" : destinationField.getText().trim();
        if (keyword.isBlank()) {
            clearDestinationFilter();
            return;
        }

        activeDestinationFilter = keyword;
        runBackground("도착지 필터를 적용하는 중입니다.", () -> filterArrivalsByDestination(keyword), arrivals -> {
            filteredArrivals = arrivals;
            showArrivalContent();
            applyArrivalSort();
            statusLabel.setText("도착지 필터 적용: " + filteredArrivals.size() + "개 노선");
        });
    }

    private ArrayList<ArrivalInfo> filterArrivalsByDestination(String keyword) throws IOException, InterruptedException {
        String term = normalize(keyword);
        ArrayList<ArrivalInfo> results = new ArrayList<>();
        for (ArrivalInfo arrival : currentArrivals) {
            ArrayList<RouteStop> routeStops = service.getRouteStops(arrival.getRouteId());
            for (RouteStop routeStop : routeStops) {
                if (normalize(routeStop.getStopName()).contains(term) || normalize(routeStop.getServiceId()).equals(term)) {
                    results.add(arrival);
                    break;
                }
            }
        }
        return results;
    }

    private void clearDestinationFilter() {
        resetDestinationFilter();
        showArrivalContent();
        applyArrivalSort();
        statusLabel.setText("도착 정보 " + currentArrivals.size() + "개 표시");
    }

    private void resetDestinationFilter() {
        activeDestinationFilter = "";
        filteredArrivals = new ArrayList<>();
        destinationField.clear();
    }

    private boolean hasDestinationFilter() {
        return !activeDestinationFilter.isBlank();
    }

    private int arrivalSortValue(ArrivalInfo arrival) {
        int seconds = parseInt(arrival.getRemainTimeSec());
        if (seconds >= 0) {
            return seconds;
        }

        int minutes = parseInt(arrival.getRemainTime());
        if (minutes >= 0) {
            return minutes * 60;
        }
        return Integer.MAX_VALUE;
    }

    private int compareRouteNumbers(String left, String right) {
        int leftNumber = firstNumber(left);
        int rightNumber = firstNumber(right);
        if (leftNumber != rightNumber) {
            return Integer.compare(leftNumber, rightNumber);
        }
        return left.compareToIgnoreCase(right);
    }

    private int firstNumber(String value) {
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (number.length() > 0) {
                break;
            }
        }
        if (number.length() == 0) {
            return Integer.MAX_VALUE;
        }
        return parseInt(number.toString());
    }

    private void showRouteContent() {
        Label busesTitle = new Label("운행 중인 버스");
        busesTitle.getStyleClass().add("section-subtitle");
        busLocationList.setMaxHeight(170);

        detailContent.getChildren().setAll(routeStopList, busesTitle, busLocationList);
        VBox.setVgrow(routeStopList, Priority.ALWAYS);
        VBox.setVgrow(busLocationList, Priority.NEVER);
    }

    private void refreshCurrentDetail() {
        if ("STOP".equals(currentFavoriteType) && selectedStop != null) {
            loadArrivalsForStop(selectedStop, false, false, highlightedArrivalRouteId);
        } else if ("ROUTE".equals(currentFavoriteType) && currentRouteId != null && currentRouteName != null) {
            loadRouteDetail(currentRouteId, currentRouteName, highlightedStopId, false);
        } else {
            statusLabel.setText("새로고침할 정보를 먼저 선택하세요.");
        }
    }

    private void toggleFavorite() {
        if (currentFavoriteType == null || currentFavoriteId == null) {
            statusLabel.setText("즐겨찾기에 추가할 항목을 먼저 선택하세요.");
            return;
        }

        boolean isFavorite = favoriteStore.toggle(currentFavoriteType, currentFavoriteId, currentFavoriteLabel);
        updateFavoriteButton();
        refreshFavoriteList();
        refreshFavoriteStateViews();
        statusLabel.setText(isFavorite ? "즐겨찾기에 추가했습니다." : "즐겨찾기에서 제거했습니다.");
    }

    private boolean isArrivalFavorite(ArrivalInfo arrival) {
        return arrival != null && favoriteStore.contains("ROUTE", arrival.getRouteId());
    }

    private void toggleArrivalFavorite(ArrivalInfo arrival) {
        if (arrival == null) {
            return;
        }

        boolean isFavorite = favoriteStore.toggle("ROUTE", arrival.getRouteId(), arrival.getBrtId() + " 노선");
        refreshFavoriteList();
        updateFavoriteButton();
        refreshFavoriteStateViews();
        statusLabel.setText(isFavorite ? "즐겨찾기에 추가했습니다." : "즐겨찾기에서 제거했습니다.");
    }

    private void refreshFavoriteStateViews() {
        arrivalList.refresh();
        favoriteList.refresh();
    }

    private void updateFavoriteButton() {
        boolean isFavorite = currentFavoriteType != null
                && currentFavoriteId != null
                && favoriteStore.contains(currentFavoriteType, currentFavoriteId);
        favoriteButton.setText(isFavorite ? "★" : "☆");
        favoriteButton.getStyleClass().remove("favorite-active");
        if (isFavorite) {
            favoriteButton.getStyleClass().add("favorite-active");
        }
    }

    private void updateTimestamp() {
        updatedAtLabel.setText(TIME_FORMATTER.format(LocalTime.now()) + " 기준");
    }

    private void clearSelection() {
        selectedStop = null;
        selectedRoute = null;
        selectedRouteResult = null;
        selectedStopResult = null;
        selectedHotPlace = null;
        selectedHotPlaceDestination = null;
        selectedFavoriteRow = null;
        highlightedStopId = null;
        currentFavoriteType = null;
        currentFavoriteId = null;
        currentFavoriteLabel = null;
        currentRouteId = null;
        currentRouteName = null;
        highlightedArrivalRouteId = null;
        searchField.clear();
        resetDestinationFilter();
        clearSearchResults();
        routeStopList.getItems().clear();
        arrivalList.getItems().clear();
        busLocationList.getItems().clear();
        hotPlaceTransitList.getItems().clear();
        hotPlaceDepartureField.clear();
        hotPlaceDestinationLabel.setText("");
        hotPlaceTransitStatusLabel.setText("");
        currentBusLocations = new ArrayList<>();
        currentBusLocationsByStopSeq = new HashMap<>();
        currentArrivals = new ArrayList<>();
        filteredArrivals = new ArrayList<>();
        detailTitle.setText("선택된 항목 없음");
        detailSubTitle.setText("검색 결과에서 정류장 또는 노선을 선택하세요.");
        detailSubTitle.setManaged(true);
        detailSubTitle.setVisible(true);
        contentTitle.setText("정보");
        routeMetaLabel.setText("");
        updatedAtLabel.setText("");
        detailRefreshButton.setDisable(true);
        navigationItems.clear();
        updateBreadcrumbs();
        updateFavoriteButton();
        Label emptyText = new Label("검색 결과에서 항목을 선택하면 정보가 표시됩니다.");
        emptyText.getStyleClass().add("empty-detail");
        detailContent.getChildren().setAll(emptyText);
        statusLabel.setText("초기화했습니다.");
    }

    private void refreshFavoriteList() {
        ArrayList<String> rows = new ArrayList<>();
        HashMap<String, ArrayList<String>> snapshot = favoriteStore.snapshot();
        for (String value : snapshot.getOrDefault("STOP", new ArrayList<>())) {
            rows.add("정류장 | " + value);
        }
        for (String value : snapshot.getOrDefault("ROUTE", new ArrayList<>())) {
            rows.add("노선 | " + value);
        }
        favoriteList.setItems(FXCollections.observableArrayList(rows));
    }

    private void selectFavorite(String row) {
        selectedFavoriteRow = row;
        favoriteList.getSelectionModel().clearSelection();
        favoriteList.refresh();
    }

    private void openFavorite(String row) {
        if (row == null || row.isBlank()) {
            return;
        }

        String[] parts = row.split("\\|", 3);
        if (parts.length < 2) {
            statusLabel.setText("즐겨찾기 정보를 읽을 수 없습니다.");
            return;
        }

        String type = parts[0].trim();
        String id = parts[1].trim();
        if (type.startsWith("정류장")) {
            BusStop stop = findStopById(id);
            if (stop == null) {
                statusLabel.setText("해당 정류장을 찾을 수 없습니다: " + id);
                return;
            }
            selectedStop = stop;
            selectedRoute = null;
            highlightedStopId = stop.getStopServiceid();
            startNavigationItem(NavigationItem.stop(stop));
            loadArrivalsForStop(stop, false);
            return;
        }

        if (type.startsWith("노선")) {
            Route route = findRouteById(id);
            if (route == null) {
                statusLabel.setText("해당 노선을 찾을 수 없습니다: " + id);
                return;
            }
            selectedRoute = route;
            selectedStop = null;
            highlightedStopId = null;
            startNavigationItem(NavigationItem.route(route.getRouteId(), route.getBrtId(), null));
            loadRouteDetail(route.getRouteId(), route.getBrtId(), null, false);
        }
    }

    private void removeFavorite(String row) {
        FavoriteReference favorite = parseFavoriteReference(row);
        if (favorite == null) {
            statusLabel.setText("즐겨찾기 정보를 읽을 수 없습니다.");
            return;
        }

        favoriteStore.remove(favorite.type, favorite.id);
        if (row.equals(selectedFavoriteRow)) {
            selectedFavoriteRow = null;
        }
        refreshFavoriteList();
        updateFavoriteButton();
        refreshFavoriteStateViews();
        statusLabel.setText("즐겨찾기에서 제거했습니다.");
    }

    private FavoriteReference parseFavoriteReference(String row) {
        if (row == null || row.isBlank()) {
            return null;
        }

        String[] parts = row.split("\\|", 3);
        if (parts.length < 2) {
            return null;
        }

        String displayType = parts[0].trim();
        String id = parts[1].trim();
        String type = displayType.startsWith("노선") ? "ROUTE" : "STOP";
        return new FavoriteReference(type, id);
    }

    private BusStop findStopById(String serviceId) {
        for (BusStop stop : service.getStops()) {
            if (stop.getStopServiceid().equals(serviceId)) {
                return stop;
            }
        }
        return null;
    }

    private Route findRouteById(String routeId) {
        for (Route route : service.getRoutes()) {
            if (route.getRouteId().equals(routeId)) {
                return route;
            }
        }
        return null;
    }

    private void refreshRecentList() {
        ArrayList<String> history = recentSearchStore.snapshot();
        refreshInlineRecentSearches(history);
    }

    private void refreshInlineRecentSearches(ArrayList<String> history) {
        inlineRecentSearchBox.getChildren().clear();
        int count = Math.min(history.size(), 3);
        inlineRecentSearchBox.setManaged(count > 0);
        inlineRecentSearchBox.setVisible(count > 0);
        if (count == 0) {
            return;
        }

        Label title = new Label("최근 검색");
        title.getStyleClass().add("inline-recent-title");
        inlineRecentSearchBox.getChildren().add(title);

        for (int i = 0; i < count; i++) {
            String entry = history.get(i);
            Button queryButton = new Button(recentSearchLabel(entry));
            queryButton.getStyleClass().add("inline-recent-query");
            queryButton.setMaxWidth(Double.MAX_VALUE);
            queryButton.setOnAction(event -> runRecentSearch(entry));
            HBox.setHgrow(queryButton, Priority.ALWAYS);

            Button removeButton = new Button("x");
            removeButton.getStyleClass().add("inline-recent-remove");
            removeButton.setOnAction(event -> {
                recentSearchStore.remove(entry);
                refreshRecentList();
            });

            HBox row = new HBox(6, queryButton, removeButton);
            row.getStyleClass().add("inline-recent-row");
            row.setAlignment(Pos.CENTER_LEFT);
            inlineRecentSearchBox.getChildren().add(row);
        }
    }

    private void runRecentSearch(String entry) {
        String query = recentSearchQuery(entry);
        if (query.isBlank()) {
            return;
        }
        searchField.setText(query);
        performSearch();
    }

    private String recentSearchLabel(String entry) {
        String query = recentSearchQuery(entry);
        return query.isBlank() ? entry : query;
    }

    private String recentSearchQuery(String entry) {
        if (entry == null) {
            return "";
        }

        String value = entry;
        int pipeIndex = value.indexOf(" | ");
        if (pipeIndex >= 0) {
            value = value.substring(pipeIndex + 3);
        }

        int colonIndex = value.indexOf(':');
        if (colonIndex >= 0 && colonIndex + 1 < value.length()) {
            value = value.substring(colonIndex + 1);
        }
        return value.trim();
    }

    private void startNavigationItem(NavigationItem item) {
        navigationItems.clear();
        navigationItems.add(item);
        updateBreadcrumbs();
    }

    private void addNavigationItem(NavigationItem item) {
        if (!navigationItems.isEmpty() && navigationItems.get(navigationItems.size() - 1).isSame(item)) {
            updateBreadcrumbs();
            return;
        }
        navigationItems.add(item);
        updateBreadcrumbs();
    }

    private void goBack() {
        if (navigationItems.size() <= 1) {
            return;
        }
        navigationItems.remove(navigationItems.size() - 1);
        NavigationItem target = navigationItems.get(navigationItems.size() - 1);
        navigateTo(target);
        updateBreadcrumbs();
    }

    private void navigateToBreadcrumb(int index) {
        if (index < 0 || index >= navigationItems.size()) {
            return;
        }
        while (navigationItems.size() > index + 1) {
            navigationItems.remove(navigationItems.size() - 1);
        }
        NavigationItem target = navigationItems.get(index);
        navigateTo(target);
        updateBreadcrumbs();
    }

    private void navigateTo(NavigationItem item) {
        if ("STOP".equals(item.type)) {
            selectedStop = item.stop;
            selectedRoute = null;
            highlightedStopId = item.stop.getStopServiceid();
            loadArrivalsForStop(item.stop, false);
        } else if ("ROUTE".equals(item.type)) {
            selectedStop = null;
            highlightedStopId = item.highlightStopId;
            loadRouteDetail(item.id, item.routeName, item.highlightStopId, false);
        } else if ("HOT_PLACE".equals(item.type)) {
            openHotPlaceTransit(item.hotPlace, false);
        }
    }

    private void updateBreadcrumbs() {
        breadcrumbBox.getChildren().clear();
        backButton.setDisable(navigationItems.size() <= 1);

        for (int i = 0; i < navigationItems.size(); i++) {
            NavigationItem item = navigationItems.get(i);
            final int index = i;
            Button crumb = new Button(item.label);
            crumb.getStyleClass().add("breadcrumb-button");
            crumb.setOnAction(event -> navigateToBreadcrumb(index));
            breadcrumbBox.getChildren().add(crumb);

            if (i < navigationItems.size() - 1) {
                Label separator = new Label(">");
                separator.getStyleClass().add("breadcrumb-separator");
                breadcrumbBox.getChildren().add(separator);
            }
        }
    }

    private <T> void runBackground(String message, BackgroundJob<T> job, SuccessHandler<T> successHandler) {
        statusLabel.setText(message);
        setBusy(true);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return job.run();
            }
        };

        task.setOnSucceeded(event -> {
            setBusy(false);
            successHandler.handle(task.getValue());
        });
        task.setOnFailed(event -> {
            setSearchLoading(false);
            setBusy(false);
            Throwable error = task.getException();
            statusLabel.setText("오류: " + (error == null ? "알 수 없는 오류" : error.getMessage()));
            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread thread = new Thread(task, "bis-ui-worker");
        thread.setDaemon(true);
        thread.start();
    }

    private void setBusy(boolean busy) {
        busyIndicator.setManaged(busy);
        busyIndicator.setVisible(busy);
    }

    private HBox spacer() {
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private String value(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private interface BackgroundJob<T> {
        T run() throws IOException, InterruptedException;
    }

    private interface SuccessHandler<T> {
        void handle(T value);
    }

    private static final class RouteDetailData {
        private ArrayList<RouteStop> routeStops;
        private ArrayList<BusLocation> busLocations;
    }

    private static final class SearchResultData {
        private final ArrayList<SearchResult> routeResults = new ArrayList<>();
        private final ArrayList<SearchResult> stopResults = new ArrayList<>();
    }

    private static final class FavoriteReference {
        private final String type;
        private final String id;

        private FavoriteReference(String type, String id) {
            this.type = type;
            this.id = id;
        }
    }

    private static final class NavigationItem {
        private final String type;
        private final String id;
        private final String label;
        private final BusStop stop;
        private final HotPlace hotPlace;
        private final String routeName;
        private final String highlightStopId;

        private NavigationItem(String type, String id, String label, BusStop stop, HotPlace hotPlace,
                               String routeName, String highlightStopId) {
            this.type = type;
            this.id = id;
            this.label = label;
            this.stop = stop;
            this.hotPlace = hotPlace;
            this.routeName = routeName;
            this.highlightStopId = highlightStopId;
        }

        private static NavigationItem stop(BusStop stop) {
            return new NavigationItem("STOP", stop.getStopServiceid(), stop.getStopKname(), stop, null, null, null);
        }

        private static NavigationItem route(String routeId, String routeName, String highlightStopId) {
            return new NavigationItem("ROUTE", routeId, routeName + " 노선", null, null, routeName, highlightStopId);
        }

        private static NavigationItem hotPlace(HotPlace hotPlace) {
            String id = hotPlace.getId() == null || hotPlace.getId().isBlank()
                    ? hotPlace.getName()
                    : hotPlace.getId();
            return new NavigationItem("HOT_PLACE", id, hotPlace.getName(), null, hotPlace, null, null);
        }

        private boolean isSame(NavigationItem other) {
            return type.equals(other.type) && id.equals(other.id);
        }
    }
}
