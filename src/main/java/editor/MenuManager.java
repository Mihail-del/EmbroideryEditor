package editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ColorPicker;

import java.io.File;
import java.util.Arrays;

/**
 * Manages all overlay menus in the embroidery editor: Save Options, Open Project,
 * Info/Guide, and the Create menu visibility.
 */
public class MenuManager {

    private static final String TEMPLATES_DIR = "src/main/resources/templates";

    private VBox saveOptionsMenu;
    private VBox openMenu;
    private VBox infoMenu;

    private final VBox createMenu;
    private final StackPane mainCanvasView;
    private final ColorPicker threadColorPicker;

    /**
     * Callback interface for menu actions that need to be handled by the controller.
     */
    public interface MenuCallbacks {
        void onSaveAsJson();
        void onExportImage(String format, boolean transparentBg, boolean animated);
        void onBrowseOpenFile();
        void onLoadRecentProject(File file);
        void onHideAllMenus();
    }

    private final MenuCallbacks callbacks;

    /**
     * Creates a new MenuManager.
     *
     * @param mainCanvasView    the main canvas view pane where menus are overlaid
     * @param createMenu        the FXML-defined create menu (managed externally)
     * @param threadColorPicker the color picker (needed for hideAllMenus)
     * @param callbacks         callback interface for actions that require controller logic
     */
    public MenuManager(StackPane mainCanvasView, VBox createMenu, ColorPicker threadColorPicker, MenuCallbacks callbacks) {
        this.mainCanvasView = mainCanvasView;
        this.createMenu = createMenu;
        this.threadColorPicker = threadColorPicker;
        this.callbacks = callbacks;
    }

    /**
     * Initializes all menus and adds them to the main canvas view.
     * Must be called after FXML injection is complete.
     */
    public void initAllMenus() {
        initSaveOptionsMenu();
        initOpenMenu();
        initInfoMenu();
    }

    // ── Save Options Menu ────────────────────────────────────────────

    private void initSaveOptionsMenu() {
        if (mainCanvasView == null) return;
        saveOptionsMenu = new VBox(20);
        saveOptionsMenu.getStyleClass().add("save-options-menu");
        saveOptionsMenu.setAlignment(Pos.CENTER);

        StackPane headerPane = new StackPane();

        Label titleLabel = new Label("Save Options");
        titleLabel.getStyleClass().add("warning-label");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        Label closeBtn = createCloseButton(e -> hideSaveOptionsMenu());

        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(-10, -10, 0, 0));

        headerPane.getChildren().addAll(titleLabel, closeBtn);

        // Template Block
        VBox templateBlock = new VBox(10);
        templateBlock.setAlignment(Pos.CENTER);
        templateBlock.getStyleClass().add("save-block-frame");
        Label templateLabel = new Label("Save as Template");
        templateLabel.getStyleClass().add("warning-label");

        Button jsonBtn = new Button(".JSON");
        jsonBtn.getStyleClass().addAll("create-grid-btn");
        jsonBtn.setPrefWidth(240);
        jsonBtn.setOnAction(e -> {
            hideSaveOptionsMenu();
            callbacks.onSaveAsJson();
        });
        templateBlock.getChildren().addAll(templateLabel, jsonBtn);

        // Image Block
        VBox imageBlock = new VBox(10);
        imageBlock.setAlignment(Pos.CENTER);
        imageBlock.getStyleClass().add("save-block-frame");
        Label imageLabel = new Label("Save as Image");
        imageLabel.getStyleClass().add("warning-label");

        HBox imageBtns = new HBox(15);
        imageBtns.setAlignment(Pos.CENTER);

        CheckBox transparentBgCheckbox = new CheckBox("Transparent bg");
        transparentBgCheckbox.getStyleClass().add("custom-checkbox");

        CheckBox animatedCheckbox = new CheckBox("Animated");
        animatedCheckbox.getStyleClass().add("custom-checkbox");

        HBox checkboxBox = new HBox(25);
        checkboxBox.setAlignment(Pos.CENTER);
        checkboxBox.getChildren().addAll(transparentBgCheckbox, animatedCheckbox);

        Button pngBtn = new Button(".PNG");
        pngBtn.getStyleClass().addAll("create-grid-btn");
        pngBtn.setStyle("-fx-padding: 10px 15px; -fx-max-width: 80px; -fx-font-size: 14px;");
        pngBtn.disableProperty().bind(animatedCheckbox.selectedProperty());
        pngBtn.setOnAction(e -> {
            hideSaveOptionsMenu();
            callbacks.onExportImage("png", transparentBgCheckbox.isSelected(), false);
        });

        Button jpgBtn = new Button(".JPG");
        jpgBtn.getStyleClass().addAll("create-grid-btn");
        jpgBtn.setStyle("-fx-padding: 10px 15px; -fx-max-width: 80px; -fx-font-size: 14px;");
        jpgBtn.disableProperty().bind(transparentBgCheckbox.selectedProperty().or(animatedCheckbox.selectedProperty()));
        jpgBtn.setOnAction(e -> {
            hideSaveOptionsMenu();
            callbacks.onExportImage("jpg", false, false);
        });

        Button gifBtn = new Button(".GIF");
        gifBtn.getStyleClass().addAll("create-grid-btn");
        gifBtn.setStyle("-fx-padding: 10px 15px; -fx-max-width: 80px; -fx-font-size: 14px;");
        gifBtn.setOnAction(e -> {
            hideSaveOptionsMenu();
            callbacks.onExportImage("gif", transparentBgCheckbox.isSelected(), animatedCheckbox.isSelected());
        });

        imageBtns.getChildren().addAll(pngBtn, jpgBtn, gifBtn);
        imageBlock.getChildren().addAll(imageLabel, checkboxBox, imageBtns);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().addAll("create-grid-btn", "warning-cancel-btn");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setOnAction(e -> hideSaveOptionsMenu());

        saveOptionsMenu.getChildren().addAll(headerPane, templateBlock, imageBlock, cancelBtn);
        saveOptionsMenu.setVisible(false);
        saveOptionsMenu.setManaged(false);

        mainCanvasView.getChildren().add(saveOptionsMenu);
    }

    public void showSaveOptionsMenu() {
        hideAllMenus();
        if (saveOptionsMenu != null) {
            saveOptionsMenu.setVisible(true);
            saveOptionsMenu.setManaged(true);
        }
    }

    public void hideSaveOptionsMenu() {
        if (saveOptionsMenu != null) {
            saveOptionsMenu.setVisible(false);
            saveOptionsMenu.setManaged(false);
        }
    }

    // ── Open Menu ────────────────────────────────────────────────────

    private void initOpenMenu() {
        if (mainCanvasView == null) return;
        openMenu = new VBox(20);
        openMenu.getStyleClass().add("save-options-menu");
        openMenu.setAlignment(Pos.CENTER);

        StackPane headerPane = new StackPane();

        Label titleLabel = new Label("Open Project");
        titleLabel.getStyleClass().add("warning-label");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        Label closeBtn = createCloseButton(e -> hideOpenMenu());

        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(-10, -10, 0, 0));

        headerPane.getChildren().addAll(titleLabel, closeBtn);

        // Section 1: Open from file
        VBox fileBlock = new VBox(10);
        fileBlock.setAlignment(Pos.CENTER);
        fileBlock.getStyleClass().add("save-block-frame");
        Label fileLabel = new Label("Open from JSON");
        fileLabel.getStyleClass().add("warning-label");
        Button browseBtn = new Button("Browse...");
        browseBtn.getStyleClass().addAll("create-grid-btn");
        browseBtn.setPrefWidth(240);
        browseBtn.setOnAction(e -> {
            hideOpenMenu();
            callbacks.onBrowseOpenFile();
        });
        fileBlock.getChildren().addAll(fileLabel, browseBtn);

        // Section 2: Recent Projects
        VBox recentBlock = new VBox(10);
        recentBlock.setAlignment(Pos.CENTER);
        recentBlock.getStyleClass().addAll("save-block-frame", "recent-projects-tab");
        Label recentLabel = new Label("Recent Projects");
        recentLabel.getStyleClass().add("warning-label");

        TilePane recentList = new TilePane();
        recentList.setAlignment(Pos.CENTER);
        recentList.setHgap(10);
        recentList.setVgap(10);
        recentList.setPrefColumns(2);

        ScrollPane scrollPane = new ScrollPane(recentList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("styled-scroll-pane");
        scrollPane.setPrefViewportHeight(140);
        scrollPane.setMaxHeight(140);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        recentBlock.getChildren().addAll(recentLabel, scrollPane);

        openMenu.getChildren().addAll(headerPane, fileBlock, recentBlock);
        openMenu.setVisible(false);
        openMenu.setManaged(false);

        openMenu.getProperties().put("recentList", recentList);

        mainCanvasView.getChildren().add(openMenu);
    }

    public void showOpenMenu() {
        hideAllMenus();
        if (openMenu != null) {
            updateRecentProjectsList();
            openMenu.setVisible(true);
            openMenu.setManaged(true);
        }
    }

    public void hideOpenMenu() {
        if (openMenu != null) {
            openMenu.setVisible(false);
            openMenu.setManaged(false);
        }
    }

    // ── Info Menu ────────────────────────────────────────────────────

    private void initInfoMenu() {
        if (mainCanvasView == null) return;
        infoMenu = new VBox(20);
        infoMenu.getStyleClass().add("save-options-menu");
        infoMenu.setAlignment(Pos.CENTER);

        StackPane headerPane = new StackPane();

        Label titleLabel = new Label("Information & Guide");
        titleLabel.getStyleClass().add("warning-label");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        Label closeBtn = createCloseButton(e -> hideInfoMenu());

        StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
        StackPane.setMargin(closeBtn, new Insets(-10, -10, 0, 0));

        headerPane.getChildren().addAll(titleLabel, closeBtn);

        // Info Content Block
        VBox contentBlock = new VBox(10);
        contentBlock.setAlignment(Pos.CENTER);
        contentBlock.getStyleClass().add("save-block-frame");

        Label infoText = new Label(
                "Vyshyvanka Editor: User Guide\n\n" +

                        "1. Basic Drawing & Tools\n" +
                        "• Drawing Stitches: Left-click to place cross-stitches.\n" +
                        "• Eraser: Toggle Eraser mode from the bottom toolbar to remove stitches.\n" +
                        "• Clear Canvas: Instantly wipes the entire board clean.\n" +
                        "• Animate: Plays a radial visual animation of your embroidery process.\n\n" +

                        "2. Thread Palette & Color Management\n" +
                        "• Select a Color: Left-click any filled color circle to make it active.\n" +
                        "• Add a New Color: Left-click an empty slot (+) to open the Color Picker.\n" +
                        "• Replace a Color: Right-click an active color to replace it.\n\n" +

                        "3. Symmetry Controls\n" +
                        "• Vertical/Horizontal: Mirrors your next drawing across the center guide lines.\n" +
                        "• Full Symmetry: Turn on both to mirror your stitches into all four quadrants.\n\n" +

                        "4. Duplicate Canvas\n" +
                        "• Left/Right: Mirrors your existed drawing across the vertical center with preview.\n" +
                        "• Up/Down: Mirrors your existed drawing across the horizontal center with preview.\n\n" +

                        "5. Project Management\n" +
                        "• Create: Start a new workspace and set a custom grid size (8x8 to 96x96).\n" +
                        "• Save: Export as Image (.PNG, .JPG, .GIF) or save as Template (.JSON).\n" +
                        "• Open: Load a previously saved .JSON file or pick from \"Recent Projects\"."
        );
        infoText.getStyleClass().add("warning-label");
        infoText.setStyle("-fx-text-alignment: left; -fx-font-size: 13px; -fx-line-spacing: 3px; -fx-wrap-text: true; -fx-padding: 20px");
        ScrollPane scrollPane = new ScrollPane(infoText);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("styled-scroll-pane");

        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        scrollPane.setPrefViewportHeight(240);
        scrollPane.setMaxHeight(240);

        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        contentBlock.getChildren().add(scrollPane);

        infoMenu.getChildren().addAll(headerPane, contentBlock);
        infoMenu.setVisible(false);
        infoMenu.setManaged(false);

        mainCanvasView.getChildren().add(infoMenu);
    }

    public void showInfoMenu() {
        hideAllMenus();
        if (infoMenu != null) {
            infoMenu.setVisible(true);
            infoMenu.setManaged(true);
        }
    }

    public void hideInfoMenu() {
        if (infoMenu != null) {
            infoMenu.setVisible(false);
            infoMenu.setManaged(false);
        }
    }

    // ── Create Menu ──────────────────────────────────────────────────

    public void showCreateMenu() {
        hideAllMenus();
        if (createMenu != null) {
            createMenu.setManaged(true);
            createMenu.setVisible(true);
        }
    }

    public void hideCreateMenu() {
        if (createMenu != null) {
            createMenu.setManaged(false);
            createMenu.setVisible(false);
        }
    }

    // ── Common ───────────────────────────────────────────────────────

    /**
     * Hides all overlay menus and the color picker.
     */
    public void hideAllMenus() {
        hideCreateMenu();
        hideSaveOptionsMenu();
        hideOpenMenu();
        hideInfoMenu();
        if (threadColorPicker != null) {
            threadColorPicker.hide();
        }
    }

    /**
     * Returns whether the create menu is currently visible.
     */
    public boolean isCreateMenuVisible() {
        return createMenu != null && createMenu.isVisible();
    }

    // ── Recent Projects ──────────────────────────────────────────────

    private void updateRecentProjectsList() {
        TilePane recentList = (TilePane) openMenu.getProperties().get("recentList");
        recentList.getChildren().clear();
        File dir = new File(TEMPLATES_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".json"));
            if (files != null && files.length > 0) {
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (File file : files) {
                    recentList.getChildren().add(createRecentProjectEntry(file));
                }
            } else {
                recentList.getChildren().add(createNoFilesLabel());
            }
        } else {
            recentList.getChildren().add(createNoFilesLabel());
        }
    }

    private HBox createRecentProjectEntry(File file) {
        HBox fileBox = new HBox(5);
        fileBox.setAlignment(Pos.CENTER_LEFT);
        fileBox.getStyleClass().add("recent-project-btn");
        fileBox.setPrefWidth(140);
        fileBox.setMaxWidth(140);

        Label nameLabel = new Label(file.getName().replace(".json", ""));
        nameLabel.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView closeIcon = new ImageView(new Image(getClass().getResource("/icons/close.png").toExternalForm()));
        closeIcon.setFitWidth(10);
        closeIcon.setFitHeight(10);

        Label deleteBtn = new Label();
        deleteBtn.setGraphic(closeIcon);
        deleteBtn.getStyleClass().add("close-btn");
        deleteBtn.setOnMouseClicked(e -> {
            e.consume();
            try {
                java.nio.file.Files.deleteIfExists(file.toPath());
                updateRecentProjectsList();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        fileBox.getChildren().addAll(nameLabel, spacer, deleteBtn);
        fileBox.setOnMouseClicked(e -> {
            hideOpenMenu();
            callbacks.onLoadRecentProject(file);
        });

        return fileBox;
    }

    private Label createNoFilesLabel() {
        Label noFiles = new Label("No recent projects found.");
        noFiles.getStyleClass().add("warning-label");
        return noFiles;
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private Label createCloseButton(javafx.event.EventHandler<javafx.scene.input.MouseEvent> onClose) {
        Label closeBtn = new Label();
        ImageView closeIcon = new ImageView(new Image(getClass().getResource("/icons/close.png").toExternalForm()));
        closeIcon.setFitWidth(14);
        closeIcon.setFitHeight(14);
        closeBtn.setGraphic(closeIcon);
        closeBtn.getStyleClass().add("close-btn");
        closeBtn.setOnMouseClicked(onClose);
        return closeBtn;
    }
}
