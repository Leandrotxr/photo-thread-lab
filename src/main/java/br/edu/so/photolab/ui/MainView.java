package br.edu.so.photolab.ui;

import br.edu.so.photolab.core.PixelBuffer;
import br.edu.so.photolab.core.RenderRequest;
import br.edu.so.photolab.core.RenderStrategy;
import br.edu.so.photolab.core.ThreadPalette;
import br.edu.so.photolab.core.ThreadedRenderEngine;
import br.edu.so.photolab.metrics.RunResult;
import br.edu.so.photolab.photo.GaussianBlurRenderer;
import br.edu.so.photolab.photo.TestImageFactory;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

public class MainView extends Application {

    private static final int TEST_WIDTH = 1600;
    private static final int TEST_HEIGHT = 900;
    private static final int CORES = Runtime.getRuntime().availableProcessors();

    private final ThreadedRenderEngine engine = new ThreadedRenderEngine();
    private final AtomicBoolean cancel = new AtomicBoolean(false);
    private final AtomicInteger tilesCompleted = new AtomicInteger(0);
    private final AtomicInteger tilesTotal = new AtomicInteger(0);
    private final AtomicBoolean previewDirty = new AtomicBoolean(false);
    private final AtomicReference<String> lastEvent = new AtomicReference<>("");

    private PixelBuffer original;
    private PixelBuffer working;
    private WritableImage previewImage;
    private AnimationTimer previewTimer;

    private Baseline baseline;

    private final ImageView imageView = new ImageView();
    private final Label placeholder = new Label("Gere uma imagem de teste ou abra uma foto");
    private final StackPane canvas = new StackPane();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Gere uma imagem de teste ou abra uma foto para comecar.");
    private final Label timeLabel = new Label("Tempo: —");
    private final Label speedupLabel = new Label("Speedup: —");
    private final Label efficiencyLabel = new Label("Eficiencia: —");
    private final Label progressLabel = new Label("Tiles: —");
    private final Label activityLabel = new Label("Aguardando...");
    private final Label coresLabel = new Label("Nucleos logicos: " + CORES);

    private final Slider threadSlider = new Slider(1, 64, Math.min(8, CORES));
    private final Label threadValue = new Label();
    private final ComboBox<Integer> tileSizeBox = new ComboBox<>();
    private final Slider radiusSlider = new Slider(1, 12, 6);
    private final Label radiusValue = new Label();
    private final ComboBox<RenderStrategy> strategyBox = new ComboBox<>();
    private final CheckBox highlightBox = new CheckBox("Colorir tiles por thread");
    private final Slider delaySlider = new Slider(0, 120, 50);
    private final Label delayValue = new Label();
    private final FlowPane legend = new FlowPane(6, 6);

    private final Button openButton = new Button("Abrir foto...");
    private final Button testButton = new Button("Gerar imagem de teste");
    private final Button renderButton = new Button("Renderizar");
    private final Button stopButton = new Button("Parar");
    private final Button benchmarkButton = new Button("Benchmark (1..64)");

    private final XYChart.Series<Number, Number> chartSeries = new XYChart.Series<>();
    private LineChart<Number, Number> chart;

    private volatile boolean busy;
    private int lastPreviewCount = -1;

    @Override
    public void start(Stage stage) {
        threadSlider.setMajorTickUnit(8);
        threadSlider.setMinorTickCount(7);
        threadSlider.setSnapToTicks(true);
        threadSlider.setShowTickMarks(true);
        threadSlider.setShowTickLabels(true);
        threadValue.textProperty().bind(threadSlider.valueProperty().asString("Threads: %.0f"));

        radiusSlider.setMajorTickUnit(1);
        radiusSlider.setMinorTickCount(0);
        radiusSlider.setSnapToTicks(true);
        radiusSlider.setShowTickMarks(true);
        radiusValue.textProperty().bind(radiusSlider.valueProperty().asString("Raio do blur: %.0f"));

        tileSizeBox.setItems(FXCollections.observableArrayList(16, 32, 64, 128));
        tileSizeBox.getSelectionModel().select(Integer.valueOf(128));

        strategyBox.setItems(FXCollections.observableArrayList(RenderStrategy.values()));
        strategyBox.getSelectionModel().select(RenderStrategy.POOL);

        highlightBox.setSelected(true);
        delaySlider.setMajorTickUnit(20);
        delaySlider.setMinorTickCount(3);
        delaySlider.setSnapToTicks(true);
        delaySlider.setShowTickMarks(true);
        delayValue.textProperty().bind(delaySlider.valueProperty().asString("Atraso visual: %.0f ms"));
        threadValue.getStyleClass().add("field-label");
        radiusValue.getStyleClass().add("field-label");
        delayValue.getStyleClass().add("field-label");

        stopButton.setDisable(true);
        renderButton.setDisable(true);
        benchmarkButton.setDisable(true);

        openButton.setMaxWidth(Double.MAX_VALUE);
        testButton.setMaxWidth(Double.MAX_VALUE);
        renderButton.setMaxWidth(Double.MAX_VALUE);
        stopButton.setMaxWidth(Double.MAX_VALUE);
        benchmarkButton.setMaxWidth(Double.MAX_VALUE);
        tileSizeBox.setMaxWidth(Double.MAX_VALUE);
        strategyBox.setMaxWidth(Double.MAX_VALUE);

        openButton.getStyleClass().add("btn-secondary");
        testButton.getStyleClass().add("btn-secondary");
        renderButton.getStyleClass().add("btn-primary");
        stopButton.getStyleClass().add("btn-danger");
        benchmarkButton.getStyleClass().add("btn-ghost");
        coresLabel.getStyleClass().add("subtitle");
        timeLabel.getStyleClass().add("metric");
        speedupLabel.getStyleClass().add("metric");
        efficiencyLabel.getStyleClass().add("metric");
        progressLabel.getStyleClass().add("metric");
        activityLabel.getStyleClass().add("activity");
        statusLabel.getStyleClass().add("status-bar");
        placeholder.getStyleClass().add("placeholder");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setProgress(0);

        NumberAxis xAxis = new NumberAxis(0, 68, 8);
        xAxis.setLabel("Threads");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Tempo (ms)");
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Tempo x threads");
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(190);
        chart.setCreateSymbols(true);
        chartSeries.setName("ms");
        chart.getData().add(chartSeries);

        VBox controls = new VBox(
                14,
                title("Photo Thread Lab"),
                coresLabel,
                section("IMAGEM", openButton, testButton),
                section("PARALELISMO", threadValue, threadSlider, labeled("Tamanho do tile"), tileSizeBox,
                        labeled("Estrategia"), strategyBox),
                section("BLUR E VISUAL", radiusValue, radiusSlider, highlightBox, delayValue, delaySlider,
                        labeled("Legenda das threads"), legend),
                renderButton,
                stopButton,
                benchmarkButton,
                section("METRICAS", progressBar, timeLabel, speedupLabel, efficiencyLabel, progressLabel, activityLabel),
                chart
        );
        controls.getStyleClass().add("sidebar");
        controls.setPrefWidth(292);
        ScrollPane controlScroll = new ScrollPane(controls);
        controlScroll.setFitToWidth(true);
        controlScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        controlScroll.setPrefWidth(312);
        controlScroll.getStyleClass().add("sidebar-scroll");

        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);
        imageView.setVisible(false);
        canvas.getChildren().addAll(placeholder, imageView);
        canvas.getStyleClass().add("canvas");
        StackPane.setAlignment(imageView, Pos.CENTER);
        StackPane.setAlignment(placeholder, Pos.CENTER);
        imageView.fitWidthProperty().bind(canvas.widthProperty().subtract(28));
        imageView.fitHeightProperty().bind(canvas.heightProperty().subtract(28));
        BorderPane.setMargin(canvas, new Insets(16, 16, 8, 8));
        HBox.setHgrow(canvas, Priority.ALWAYS);
        VBox.setVgrow(canvas, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setLeft(controlScroll);
        root.setCenter(canvas);
        root.setBottom(statusLabel);

        openButton.setOnAction(e -> openPhoto(stage));
        testButton.setOnAction(e -> generateTestImage());
        renderButton.setOnAction(e -> startRender(false));
        stopButton.setOnAction(e -> cancel.set(true));
        benchmarkButton.setOnAction(e -> startRender(true));

        previewTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                int done = tilesCompleted.get();
                int total = tilesTotal.get();
                refreshPreview(busy, done);
                if (total > 0 && busy) {
                    progressLabel.setText("Tiles: " + done + " / " + total);
                    progressBar.setProgress(done / (double) total);
                    String event = lastEvent.get();
                    if (event != null && !event.isBlank()) {
                        activityLabel.setText(event);
                    }
                }
            }
        };
        previewTimer.start();
        rebuildLegend((int) Math.round(threadSlider.getValue()));

        stage.setTitle("Photo Thread Lab");
        Scene scene = new Scene(root, 1060, 680);
        var css = MainView.class.getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setMinWidth(920);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(e -> {
            cancel.set(true);
            if (previewTimer != null) {
                previewTimer.stop();
            }
        });
        stage.show();
    }

    private void openPhoto(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Abrir foto");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            var image = ImageIO.read(file);
            if (image == null) {
                statusLabel.setText("Arquivo de imagem invalido.");
                return;
            }
            loadBuffer(PixelBuffer.fromBufferedImage(image), file.getName());
        } catch (Exception ex) {
            statusLabel.setText("Falha ao abrir foto: " + ex.getMessage());
        }
    }

    private void loadBuffer(PixelBuffer buffer, String name) {
        original = buffer;
        working = buffer.copy();
        previewImage = new WritableImage(buffer.width(), buffer.height());
        imageView.setImage(previewImage);
        imageView.setVisible(true);
        placeholder.setVisible(false);
        progressBar.setProgress(0);
        baseline = null;
        chartSeries.getData().clear();
        lastPreviewCount = -1;
        refreshPreview(true, 0);
        renderButton.setDisable(false);
        benchmarkButton.setDisable(false);
        timeLabel.setText("Tempo: —");
        speedupLabel.setText("Speedup: —");
        efficiencyLabel.setText("Eficiencia: —");
        progressLabel.setText("Tiles: —");
        activityLabel.setText("Aguardando renderizacao...");
        statusLabel.setText("Carregado: " + name + " (" + buffer.width() + "x" + buffer.height()
                + "). Clique em Renderizar e veja os tiles coloridos aparecendo.");
    }

    private void startRender(boolean benchmark) {
        if (original == null || busy) {
            return;
        }
        final JobSettings job = benchmark ? currentSettings().forMeasurement() : currentSettings();
        rebuildLegend(job.threads());
        lastEvent.set("Iniciando...");
        busy = true;
        cancel.set(false);
        setControlsDisabled(true);
        if (benchmark) {
            chartSeries.getData().clear();
        }
        Thread coordinator = new Thread(() -> {
            try {
                if (benchmark) {
                    runBenchmark(job);
                } else {
                    RunResult result = runOnce(job);
                    Platform.runLater(() -> showResult(result, job, false));
                }
            } catch (Exception ex) {
                Platform.runLater(() -> statusLabel.setText("Erro: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    busy = false;
                    setControlsDisabled(false);
                    refreshPreview(true, tilesCompleted.get());
                });
            }
        }, "render-coordinator");
        coordinator.setDaemon(true);
        coordinator.start();
    }

    private void runBenchmark(JobSettings settings) {
        Platform.runLater(() -> statusLabel.setText("Aquecimento do JIT (descartado)..."));
        runOnce(settings);
        if (cancel.get()) {
            Platform.runLater(() -> statusLabel.setText("Benchmark cancelado."));
            return;
        }
        int[] points = {1, 2, 4, 8, 16, 32, 64};
        Long baselineMs = null;
        for (int n : points) {
            if (cancel.get()) {
                Platform.runLater(() -> statusLabel.setText("Benchmark cancelado."));
                return;
            }
            int threads = n;
            Platform.runLater(() -> {
                threadSlider.setValue(threads);
                rebuildLegend(threads);
                statusLabel.setText("Benchmark: " + threads + " thread(s)...");
            });
            RunResult result = runOnce(settings.withThreads(threads));
            if (result.threads() == 1 && !result.cancelled()) {
                baselineMs = result.elapsedMs();
            }
            Long baselineCopy = baselineMs;
            Platform.runLater(() -> {
                chartSeries.getData().add(new XYChart.Data<>(result.threads(), result.elapsedMs()));
                showResult(result, settings.withThreads(threads), true);
                if (baselineCopy != null) {
                    applySpeedup(result, baselineCopy);
                }
            });
        }
        Platform.runLater(() -> statusLabel.setText(
                "Benchmark concluido. O vale da curva e o melhor N; a subida mostra excesso de threads."));
    }

    private void generateTestImage() {
        statusLabel.setText("Gerando imagem de teste...");
        Thread generator = new Thread(() -> {
            PixelBuffer buffer = TestImageFactory.colorful(TEST_WIDTH, TEST_HEIGHT);
            Platform.runLater(() -> loadBuffer(buffer, "Imagem de teste " + TEST_WIDTH + "x" + TEST_HEIGHT));
        }, "test-image-generator");
        generator.setDaemon(true);
        generator.start();
    }

    private RunResult runOnce(JobSettings settings) {
        previewDirty.set(true);
        GaussianBlurRenderer renderer = new GaussianBlurRenderer(settings.radius());
        RenderRequest request = new RenderRequest(
                settings.threads(),
                settings.tileSize(),
                settings.strategy(),
                renderer,
                settings.highlight(),
                settings.delayMs()
        );
        int expectedTiles = switch (request.strategy()) {
            case STATIC_STRIPS -> Math.min(settings.threads(), original.height());
            default -> ((original.width() + request.tileSize() - 1) / request.tileSize())
                    * ((original.height() + request.tileSize() - 1) / request.tileSize());
        };
        tilesTotal.set(expectedTiles);
        tilesCompleted.set(0);
        lastEvent.set("Renderizando com " + settings.threads() + " thread(s)...");
        RunResult result = engine.render(request, original, working, cancel, tilesCompleted, lastEvent);
        previewDirty.set(true);
        return result;
    }

    private void showResult(RunResult result, JobSettings settings, boolean fromBenchmark) {
        refreshPreview(true, result.tilesCompleted());
        progressLabel.setText("Tiles: " + result.tilesCompleted() + " / " + result.tileCount());
        progressBar.setProgress(result.tileCount() == 0 ? 0 : result.tilesCompleted() / (double) result.tileCount());
        activityLabel.setText(result.cancelled() ? "Cancelado." : "Concluido.");
        if (result.cancelled()) {
            timeLabel.setText("Tempo: " + result.elapsedMs() + " ms (cancelado)");
            statusLabel.setText("Render cancelado apos " + result.elapsedMs() + " ms.");
            return;
        }
        timeLabel.setText("Tempo: " + result.elapsedMs() + " ms"
                + (settings.delayMs() > 0 ? " (inclui atraso visual)" : ""));
        if (result.threads() == 1) {
            baseline = Baseline.from(original, settings, result.elapsedMs());
        }
        if (baseline != null && baseline.matches(original, settings)) {
            applySpeedup(result, baseline.elapsedMs());
        } else {
            speedupLabel.setText("Speedup: rode com 1 thread para calibrar");
            efficiencyLabel.setText("Eficiencia: —");
        }
        if (!fromBenchmark) {
            statusLabel.setText(describe(result));
        }
    }

    private void applySpeedup(RunResult result, long baselineMs) {
        double speedup = result.speedupAgainst(baselineMs);
        double efficiency = result.efficiencyAgainst(baselineMs);
        speedupLabel.setText(String.format("Speedup: %.2fx  (vs 1 thread = %d ms)", speedup, baselineMs));
        efficiencyLabel.setText(String.format("Eficiencia: %.0f%%", efficiency * 100));
    }

    private String describe(RunResult result) {
        String extra = "";
        if (result.threads() < CORES) {
            extra = " Poucas threads: CPU tende a ficar ociosa.";
        } else if (result.threads() <= CORES * 2) {
            extra = " Perto dos nucleos: costuma ser o melhor ponto.";
        } else {
            extra = " Excesso: oversubscription e troca de contexto podem piorar.";
        }
        if (result.strategy() == RenderStrategy.THREAD_PER_TILE) {
            extra += " Modo ingenuo cria uma thread por tile.";
        }
        return result.tileCount() + " tiles em " + result.elapsedMs() + " ms com "
                + result.threads() + " thread(s)." + extra;
    }

    private void refreshPreview(boolean force, int completedTiles) {
        if (working == null || previewImage == null) {
            return;
        }
        if (force || completedTiles != lastPreviewCount || previewDirty.getAndSet(false)) {
            working.writeTo(previewImage);
            lastPreviewCount = completedTiles;
        }
    }

    private JobSettings currentSettings() {
        return new JobSettings(
                (int) Math.round(threadSlider.getValue()),
                tileSize(),
                blurRadius(),
                strategy(),
                highlightBox.isSelected(),
                (int) Math.round(delaySlider.getValue())
        );
    }

    private void setControlsDisabled(boolean rendering) {
        openButton.setDisable(rendering);
        testButton.setDisable(rendering);
        renderButton.setDisable(rendering || original == null);
        benchmarkButton.setDisable(rendering || original == null);
        stopButton.setDisable(!rendering);
        threadSlider.setDisable(rendering);
        tileSizeBox.setDisable(rendering);
        radiusSlider.setDisable(rendering);
        strategyBox.setDisable(rendering);
        highlightBox.setDisable(rendering);
        delaySlider.setDisable(rendering);
    }

    private int blurRadius() {
        return (int) Math.round(radiusSlider.getValue());
    }

    private int tileSize() {
        Integer value = tileSizeBox.getValue();
        return value == null ? 128 : value;
    }

    private RenderStrategy strategy() {
        RenderStrategy value = strategyBox.getValue();
        return value == null ? RenderStrategy.POOL : value;
    }

    private static Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("brand");
        return label;
    }

    private static Label labeled(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private static VBox section(String name, Node... children) {
        Label heading = new Label(name);
        heading.getStyleClass().add("section-title");
        VBox box = new VBox(8);
        box.getStyleClass().add("section");
        box.getChildren().add(heading);
        box.getChildren().addAll(children);
        return box;
    }

    private void rebuildLegend(int threadCount) {
        legend.getChildren().clear();
        int shown = Math.min(threadCount, ThreadPalette.size());
        for (int i = 0; i < shown; i++) {
            int argb = ThreadPalette.color(i);
            Rectangle swatch = new Rectangle(12, 12, Color.rgb((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF));
            swatch.setArcWidth(6);
            swatch.setArcHeight(6);
            Label name = new Label("T" + i);
            name.getStyleClass().add("legend-name");
            HBox item = new HBox(6, swatch, name);
            item.setAlignment(Pos.CENTER_LEFT);
            item.getStyleClass().add("legend-chip");
            legend.getChildren().add(item);
        }
        if (threadCount > shown) {
            legend.getChildren().add(new Label("+" + (threadCount - shown)));
        }
    }

    private record JobSettings(
            int threads,
            int tileSize,
            int radius,
            RenderStrategy strategy,
            boolean highlight,
            int delayMs
    ) {
        JobSettings withThreads(int n) {
            return new JobSettings(n, tileSize, radius, strategy, highlight, delayMs);
        }

        JobSettings forMeasurement() {
            return new JobSettings(threads, tileSize, radius, strategy, false, 0);
        }
    }

    private record Baseline(int width, int height, int radius, int tileSize, RenderStrategy strategy, long elapsedMs) {
        static Baseline from(PixelBuffer image, JobSettings settings, long elapsedMs) {
            return new Baseline(image.width(), image.height(), settings.radius(), settings.tileSize(),
                    settings.strategy(), elapsedMs);
        }

        boolean matches(PixelBuffer image, JobSettings settings) {
            return width == image.width()
                    && height == image.height()
                    && radius == settings.radius()
                    && tileSize == settings.tileSize()
                    && strategy == settings.strategy();
        }
    }
}
