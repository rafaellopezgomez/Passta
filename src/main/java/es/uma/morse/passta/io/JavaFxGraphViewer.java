package es.uma.morse.passta.io;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public class JavaFxGraphViewer {

	private final Pane graphPane = new Pane();
	private final Group graphGroup = new Group(graphPane);
	private final Pane rootPane = new Pane();

	private double panStartX;
	private double panStartY;
	private double groupStartX;
	private double groupStartY;

	private static final double ZOOM_FACTOR = 1.15;

	private final List<StateView> displayedStates = new ArrayList<>();

	private final List<EdgeView> displayedEdges = new ArrayList<>();

	private final Label detailsTitle = new Label("Details");
	private final Label detailsContent = new Label("Select an edge");

	private SelectableView selectedView;
	
	private static final Object JAVAFX_LOCK = new Object();

	private static boolean javafxStarted;

	private interface SelectableView {
		void setSelected(boolean selected);
	}

	public static void open(GraphModel graph) {
	    if (graph == null) {
	        throw new IllegalArgumentException(
	            "The graph cannot be null"
	        );
	    }

	    startJavaFxIfNecessary();

	    Platform.runLater(() -> {
	        JavaFxGraphViewer viewer =
	            new JavaFxGraphViewer();

	        viewer.showGraph(graph);
	    });
	}
	
	private static void startJavaFxIfNecessary() {
	    synchronized (JAVAFX_LOCK) {
	        if (javafxStarted) {
	            return;
	        }

	        try {
	            Platform.startup(() -> {
	                // JavaFX initialized
	            });
	        } catch (IllegalStateException exception) {
	        }

	        javafxStarted = true;
	    }
	}
	
	private void showGraph(GraphModel graph) {
	    rootPane.setStyle(
	        "-fx-background-color: #fafafa;"
	    );

	    graphPane.setStyle(
	        "-fx-background-color: transparent;"
	    );

	    rootPane.getChildren().add(graphGroup);

	    createGraph(graph);
	    configureZoom(rootPane);
	    configurePan(rootPane);

	    HBox controls = createControls();
	    rootPane.getChildren().add(controls);

	    VBox detailsPanel = createDetailsPanel();
	    rootPane.getChildren().add(detailsPanel);

	    Scene scene = new Scene(
	        rootPane,
	        1200,
	        800,
	        Color.web("#fafafa")
	    );

	    configureKeyboard(scene);

	    Stage stage = new Stage();

	    stage.setTitle("PASSTA Graph Viewer");
	    stage.setScene(scene);
	    stage.setMinWidth(700.0);
	    stage.setMinHeight(500.0);
	    stage.show();

	    Platform.runLater(() -> {
	        rootPane.applyCss();
	        rootPane.layout();
	        fitGraphToWindow();
	    });
	}
	
	private void configureKeyboard(Scene scene) {
	    scene.setOnKeyPressed(event -> {
	        if (event.isControlDown()
	                && event.getCode()
	                == javafx.scene.input.KeyCode.DIGIT0) {

	            fitGraphToWindow();
	            event.consume();
	            return;
	        }

	        if (event.getCode()
	                == javafx.scene.input.KeyCode.ESCAPE) {

	            clearSelection();
	            event.consume();
	        }
	    });
	}

	private void selectView(SelectableView view) {
		if (selectedView != null) {
			selectedView.setSelected(false);
		}

		selectedView = view;

		if (selectedView != null) {
			selectedView.setSelected(true);
		}
	}

	private HBox createControls() {
		Button zoomInButton = new Button("+");
		Button zoomOutButton = new Button("-");
		Button fitButton = new Button("Fit");
		Button resetButton = new Button("Reset");

		zoomInButton.setTooltip(new Tooltip("Zoom in"));

		zoomOutButton.setTooltip(new Tooltip("Zoom out"));

		fitButton.setTooltip(new Tooltip("Adjust the automaton to fit the window"));

		zoomInButton.setOnAction(event -> zoomAtCenter(ZOOM_FACTOR));

		zoomOutButton.setOnAction(event -> zoomAtCenter(1.0 / ZOOM_FACTOR));

		fitButton.setOnAction(event -> fitGraphToWindow());

		resetButton.setTooltip(new Tooltip("Reset positions"));

		resetButton.setOnAction(event -> resetGraphLayout());

		HBox controls = new HBox(6.0, zoomInButton, zoomOutButton, new Separator(), fitButton, resetButton);
		controls.setPadding(new Insets(8.0));

		controls.setStyle("-fx-background-color: rgba(255, 255, 255, 0.95);" + "-fx-background-radius: 8;"
				+ "-fx-border-color: #cccccc;" + "-fx-border-radius: 8;" + "-fx-effect: dropshadow("
				+ "gaussian, rgba(0, 0, 0, 0.2), 8, 0, 0, 2" + ");");

		controls.setLayoutX(12.0);
		controls.setLayoutY(12.0);

		controls.setOnMousePressed(event -> event.consume());

		controls.setOnMouseDragged(event -> event.consume());

		controls.setOnScroll(event -> event.consume());

		return controls;
	}

	private void clearSelection() {
		if (selectedView != null) {
			selectedView.setSelected(false);
			selectedView = null;
		}

		detailsTitle.setText("Details");
		detailsContent.setText("Select one location or edge.");
	}

	private void zoomAtCenter(double factor) {
		double oldScale = graphGroup.getScaleX();

		double newScale = clamp(oldScale * factor, 0.15, 5.0);

		double centerX = rootPane.getWidth() / 2.0;
		double centerY = rootPane.getHeight() / 2.0;

		Point2D centerInGraph = graphGroup.sceneToLocal(rootPane.localToScene(centerX, centerY));

		graphGroup.setScaleX(newScale);
		graphGroup.setScaleY(newScale);

		Point2D centerAfterScale = graphGroup.localToScene(centerInGraph);

		Point2D centerInScene = rootPane.localToScene(centerX, centerY);

		graphGroup.setTranslateX(graphGroup.getTranslateX() + centerInScene.getX() - centerAfterScale.getX());

		graphGroup.setTranslateY(graphGroup.getTranslateY() + centerInScene.getY() - centerAfterScale.getY());
	}

	private void fitGraphToWindow() {
		if (graphPane.getChildren().isEmpty()) {
			return;
		}

		double availableWidth = rootPane.getWidth();
		double availableHeight = rootPane.getHeight();

		if (availableWidth <= 0.0 || availableHeight <= 0.0) {
			return;
		}

		Bounds graphBounds = graphPane.getBoundsInLocal();

		if (graphBounds.getWidth() <= 0.0 || graphBounds.getHeight() <= 0.0) {
			return;
		}

		double margin = 80.0;

		double usableWidth = Math.max(1.0, availableWidth - margin * 2.0);

		double usableHeight = Math.max(1.0, availableHeight - margin * 2.0);

		double scaleX = usableWidth / graphBounds.getWidth();

		double scaleY = usableHeight / graphBounds.getHeight();

		double scale = Math.min(scaleX, scaleY);

		scale = clamp(scale, 0.15, 2.0);

		graphGroup.setTranslateX(0.0);
		graphGroup.setTranslateY(0.0);

		graphGroup.setScaleX(scale);
		graphGroup.setScaleY(scale);

		Bounds transformedBounds = graphGroup.getBoundsInParent();

		double transformedCenterX = transformedBounds.getMinX() + transformedBounds.getWidth() / 2.0;

		double transformedCenterY = transformedBounds.getMinY() + transformedBounds.getHeight() / 2.0;

		double windowCenterX = availableWidth / 2.0;

		double windowCenterY = availableHeight / 2.0;

		graphGroup.setTranslateX(windowCenterX - transformedCenterX);

		graphGroup.setTranslateY(windowCenterY - transformedCenterY);
	}

	private VBox createDetailsPanel() {
		detailsTitle.setStyle("-fx-font-size: 14px;" + "-fx-font-weight: bold;" + "-fx-text-fill: #263238;");

		detailsContent.setWrapText(true);
		detailsContent.setMaxWidth(260.0);

		detailsContent.setStyle("-fx-font-size: 12px;" + "-fx-text-fill: #37474f;");

		VBox panel = new VBox(8.0, detailsTitle, detailsContent);

		panel.setPadding(new Insets(12.0));
		panel.setPrefWidth(280.0);
		panel.setMaxWidth(280.0);

		panel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.96);" + "-fx-background-radius: 8;"
				+ "-fx-border-color: #cccccc;" + "-fx-border-radius: 8;" + "-fx-effect: dropshadow("
				+ "gaussian, rgba(0, 0, 0, 0.2), 8, 0, 0, 2" + ");");

		panel.layoutXProperty().bind(rootPane.widthProperty().subtract(panel.prefWidthProperty()).subtract(12.0));

		panel.setLayoutY(12.0);

		panel.setOnMousePressed(event -> event.consume());
		panel.setOnMouseDragged(event -> event.consume());
		panel.setOnScroll(event -> event.consume());

		return panel;
	}

	private void createGraph(GraphModel graph) {
		Map<String, StateView> stateViews = new HashMap<>();

		for (StateModel state : graph.states()) {
			StateView view = new StateView(state.id(), state.label(), state.x(), state.y(), state.initial(),
					selectedState -> {
						selectView(selectedState);

						showStateDetails(state.id(), state.label(), state.initial());
					});
			displayedStates.add(view);
			stateViews.put(state.id(), view);
		}

		Map<String, List<EdgeModel>> edgeGroups = new LinkedHashMap<>();

		for (EdgeModel edge : graph.edges()) {
			String groupKey = createEdgeGroupKey(edge);

			edgeGroups.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(edge);
		}

		for (List<EdgeModel> group : edgeGroups.values()) {
			int parallelCount = group.size();

			for (int parallelIndex = 0; parallelIndex < parallelCount; parallelIndex++) {
				EdgeModel edge = group.get(parallelIndex);

				StateView source = stateViews.get(edge.sourceId());
				StateView target = stateViews.get(edge.targetId());

				if (source == null || target == null) {
					throw new IllegalArgumentException("Invalid edge " + edge.id() + ": source=" + edge.sourceId()
							+ ", target=" + edge.targetId());
				}

				EdgeView[] edgeViewReference = new EdgeView[1];

				EdgeView edgeView = new EdgeView(edge.id(), source, target, edge.label(), edge.description(),
						parallelIndex, parallelCount, () -> {
							selectView(edgeViewReference[0]);

							showEdgeDetails(edge.id(), edge.description());
						});
				edgeViewReference[0] = edgeView;
				displayedEdges.add(edgeView);
				graphPane.getChildren().add(edgeView);
			}
		}

		for (StateView stateView : stateViews.values()) {
			if (stateView.isInitial()) {
				InitialStateMarker marker = new InitialStateMarker(stateView);

				graphPane.getChildren().add(marker);
			}
		}

		graphPane.getChildren().addAll(stateViews.values());
	}

	private void showEdgeDetails(String edgeId, String description) {
		detailsTitle.setText("Edge " + edgeId);

		if (description == null || description.isBlank()) {
			detailsContent.setText("Not available information.");
		} else {
			detailsContent.setText(description);
		}
	}

	private void resetGraphLayout() {
		clearSelection();

		for (EdgeView edge : displayedEdges) {
			edge.resetShape();
		}

		for (StateView state : displayedStates) {
			state.resetPosition();
		}

		Platform.runLater(() -> {
			rootPane.applyCss();
			rootPane.layout();
			fitGraphToWindow();
		});
	}

	public static final class InitialStateMarker extends Group {

		private static final double LINE_LENGTH = 45.0;
		private static final double GAP = 6.0;
		private static final double ARROW_SIZE = 10.0;

		private static final Color COLOR = Color.web("#2f4f6f");

		private final Line line = new Line();
		private final Polygon arrow = new Polygon();

		public InitialStateMarker(StateView state) {
			line.setStroke(COLOR);
			line.setStrokeWidth(2.0);
			line.setMouseTransparent(true);

			arrow.setFill(COLOR);
			arrow.setMouseTransparent(true);

			getChildren().addAll(line, arrow);

			Runnable updateMarker = () -> updatePosition(state);

			state.layoutXProperty().addListener((observable, oldValue, newValue) -> updateMarker.run());

			state.layoutYProperty().addListener((observable, oldValue, newValue) -> updateMarker.run());

			Platform.runLater(updateMarker);
		}

		private void updatePosition(StateView state) {
			double centerX = state.getCenterX();
			double centerY = state.getCenterY();
			double radius = state.getRadius();

			double endX = centerX - radius - GAP;
			double endY = centerY;

			double startX = endX - LINE_LENGTH;
			double startY = centerY;

			line.setStartX(startX);
			line.setStartY(startY);
			line.setEndX(endX);
			line.setEndY(endY);

			arrow.getPoints().setAll(endX, endY, endX - ARROW_SIZE, endY - ARROW_SIZE * 0.55, endX - ARROW_SIZE,
					endY + ARROW_SIZE * 0.55);
		}
	}

	private String createEdgeGroupKey(EdgeModel edge) {
		String sourceId = edge.sourceId();
		String targetId = edge.targetId();

		if (sourceId.equals(targetId)) {
			return sourceId + "->" + targetId;
		}

		if (sourceId.compareTo(targetId) < 0) {
			return sourceId + "<->" + targetId;
		}

		return targetId + "<->" + sourceId;
	}

	private void configureZoom(Pane root) {
		root.setOnScroll(event -> {
			double oldScale = graphGroup.getScaleX();
			double factor = event.getDeltaY() > 0 ? 1.1 : 1.0 / 1.1;

			double newScale = clamp(oldScale * factor, 0.15, 5.0);

			Point2D mouseInGroup = graphGroup.sceneToLocal(event.getSceneX(), event.getSceneY());

			graphGroup.setScaleX(newScale);
			graphGroup.setScaleY(newScale);

			Point2D mouseAfterScale = graphGroup.localToScene(mouseInGroup);

			graphGroup.setTranslateX(graphGroup.getTranslateX() + event.getSceneX() - mouseAfterScale.getX());

			graphGroup.setTranslateY(graphGroup.getTranslateY() + event.getSceneY() - mouseAfterScale.getY());

			event.consume();
		});
	}

	private void configurePan(Pane root) {
		root.setOnMousePressed(event -> {
			if (event.getButton() != MouseButton.PRIMARY) {
				return;
			}

			if (event.getTarget() != root && event.getTarget() != graphPane) {
				return;
			}

			clearSelection();

			panStartX = event.getSceneX();
			panStartY = event.getSceneY();

			groupStartX = graphGroup.getTranslateX();
			groupStartY = graphGroup.getTranslateY();

			root.setStyle("-fx-cursor: closed-hand;");
		});

		root.setOnMouseDragged(event -> {
			if (!event.isPrimaryButtonDown()) {
				return;
			}

			if (event.getTarget() != root && event.getTarget() != graphPane) {
				return;
			}

			graphGroup.setTranslateX(groupStartX + event.getSceneX() - panStartX);

			graphGroup.setTranslateY(groupStartY + event.getSceneY() - panStartY);

			event.consume();
		});

		root.setOnMouseReleased(event -> {
			root.setStyle("-fx-cursor: default;");
		});
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	public static final class StateView extends StackPane implements SelectableView {

		private static final double MIN_RADIUS = 38.0;
		private static final double MAX_RADIUS = 90.0;

		private final double radius;

		private final String stateId;

		private double dragOffsetX;
		private double dragOffsetY;

		private final boolean initial;

		private final double initialX;
		private final double initialY;

		private final Circle circle = new Circle();

		private boolean dragged;

		public StateView(String stateId, String labelText, double x, double y, boolean initial,
				Consumer<StateView> onSelected) {

			this.stateId = stateId;
			this.radius = calculateRadius(labelText);
			this.initial = initial;
			this.initialX = x;
			this.initialY = y;

			circle.setRadius(radius);
			circle.setFill(Color.WHITE);
			circle.setStroke(Color.web("#2f4f6f"));
			circle.setStrokeWidth(2.0);

			Label label = new Label(labelText);
			label.setMouseTransparent(true);
			label.setWrapText(true);
			label.setMaxWidth(radius * 1.6);

			label.setStyle("-fx-alignment: center;" + "-fx-text-alignment: center;");

			getChildren().addAll(circle, label);

			setLayoutX(x - radius);
			setLayoutY(y - radius);

			setStyle("-fx-cursor: hand;");

			configureSelection(onSelected);
			configureDragging();
		}

		@Override
		public void setSelected(boolean selected) {
			if (selected) {
				circle.setStroke(Color.web("#1976d2"));
				circle.setStrokeWidth(4.0);
				circle.setFill(Color.web("#e3f2fd"));
			} else {
				circle.setStroke(Color.web("#2f4f6f"));
				circle.setStrokeWidth(2.0);
				circle.setFill(Color.WHITE);
			}
		}

		private void configureSelection(Consumer<StateView> onSelected) {
			setOnMouseClicked(event -> {
				if (dragged) {
					dragged = false;
					event.consume();
					return;
				}

				if (event.getClickCount() == 1) {
					onSelected.accept(this);
					event.consume();
				}
			});
		}

		public void resetPosition() {
			setLayoutX(initialX - radius);
			setLayoutY(initialY - radius);
		}

		private static double calculateRadius(String labelText) {
			if (labelText == null || labelText.isBlank()) {
				return MIN_RADIUS;
			}

			String[] lines = labelText.split("\\R");

			int longestLineLength = 0;

			for (String line : lines) {
				longestLineLength = Math.max(longestLineLength, line.length());
			}

			double widthRadius = 18.0 + longestLineLength * 3.7;
			double heightRadius = 24.0 + lines.length * 8.0;

			double calculatedRadius = Math.max(widthRadius, heightRadius);

			return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, calculatedRadius));
		}

		public String getStateId() {
			return stateId;
		}

		public boolean isInitial() {
			return initial;
		}

		public double getCenterX() {
			return getLayoutX() + radius;
		}

		public double getCenterY() {
			return getLayoutY() + radius;
		}

		public double getRadius() {
			return radius;
		}

		private void configureDragging() {
			setOnMousePressed(event -> {
				if (event.getButton() != MouseButton.PRIMARY) {
					return;
				}
				dragged = false;
				Point2D parentPoint = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());

				dragOffsetX = parentPoint.getX() - getLayoutX();
				dragOffsetY = parentPoint.getY() - getLayoutY();

				toFront();
				setStyle("-fx-cursor: closed-hand;");

				event.consume();
			});

			setOnMouseDragged(event -> {
				dragged = true;
				Point2D parentPoint = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());
				setLayoutX(parentPoint.getX() - dragOffsetX);
				setLayoutY(parentPoint.getY() - dragOffsetY);

				event.consume();
			});

			setOnMouseReleased(event -> {
				setStyle("-fx-cursor: hand;");
				event.consume();
			});
		}
	}

	private void showStateDetails(String stateId, String label, boolean initial) {
		detailsTitle.setText("Location L" + stateId);

		StringBuilder content = new StringBuilder();

		if (initial) {
			content.append("Initial location");
		} else {
			content.append("Location");
		}

		String stateInformation = removeStateIdentifier(label);

		if (!stateInformation.isBlank()) {
			content.append("\n\n");
			content.append(stateInformation);
		}

		detailsContent.setText(content.toString());
	}

	private String removeStateIdentifier(String label) {
		if (label == null || label.isBlank()) {
			return "";
		}

		int firstLineBreak = label.indexOf('\n');

		if (firstLineBreak < 0) {
			return "";
		}

		return label.substring(firstLineBreak + 1).trim();
	}

	public static final class EdgeView extends Group implements SelectableView {

		private static final double ARROW_SIZE = 10.0;
		private static final double LOOP_HEIGHT = 90.0;
		private static final double LOOP_WIDTH = 55.0;

		private static final Color NORMAL_COLOR = Color.web("#555555");
		private static final Color HOVER_COLOR = Color.web("#1976d2");

		private final CubicCurve curve = new CubicCurve();
		private final Polygon arrow = new Polygon();
		private final CubicCurve hoverCurve = new CubicCurve();

		private final String edgeId;
		private final Label edgeLabel = new Label();

		private final int parallelIndex;
		private final int parallelCount;

		private static final double PARALLEL_SPACING = 45.0;
		private static final double LOOP_SPACING = 25.0;

		private double userOffset = 0.0;

		private double loopOffsetX = 0.0;
		private double loopOffsetY = 0.0;

		private double dragStartLoopOffsetX;
		private double dragStartLoopOffsetY;

		private double dragStartOffset;
		private Point2D dragStartPoint;

		private boolean selected;

		public EdgeView(String edgeId, StateView source, StateView target, String label, String description,
				int parallelIndex, int parallelCount, Runnable onSelected) {

			this.edgeId = edgeId;
			this.parallelIndex = parallelIndex;
			this.parallelCount = parallelCount;

			edgeLabel.setText(label);
			edgeLabel.setWrapText(true);
			edgeLabel.setMaxWidth(220.0);
			edgeLabel.setMouseTransparent(true);

			edgeLabel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.90);" + "-fx-background-radius: 4;"
					+ "-fx-padding: 3 5 3 5;" + "-fx-font-size: 11px;" + "-fx-text-fill: #333333;");

			hoverCurve.setFill(Color.TRANSPARENT);
			hoverCurve.setStroke(Color.TRANSPARENT);
			hoverCurve.setStrokeWidth(18.0);
			hoverCurve.setMouseTransparent(false);

			curve.setFill(Color.TRANSPARENT);
			curve.setStroke(NORMAL_COLOR);
			curve.setStrokeWidth(1.8);

			hoverCurve.startXProperty().bind(curve.startXProperty());
			hoverCurve.startYProperty().bind(curve.startYProperty());

			hoverCurve.controlX1Property().bind(curve.controlX1Property());
			hoverCurve.controlY1Property().bind(curve.controlY1Property());

			hoverCurve.controlX2Property().bind(curve.controlX2Property());
			hoverCurve.controlY2Property().bind(curve.controlY2Property());

			hoverCurve.endXProperty().bind(curve.endXProperty());
			hoverCurve.endYProperty().bind(curve.endYProperty());

			hoverCurve.setStyle("-fx-cursor: hand;");

			arrow.setFill(NORMAL_COLOR);

			getChildren().addAll(hoverCurve, curve, arrow, edgeLabel);

			Runnable updateEdge = () -> {
				if (source == target) {
					updateLoop(source);
				} else {
					updateNormalEdge(source, target);
				}

				updateArrowHead();
				updateEdgeLabel();
			};

			source.layoutXProperty().addListener((observable, oldValue, newValue) -> updateEdge.run());

			source.layoutYProperty().addListener((observable, oldValue, newValue) -> updateEdge.run());

			if (source != target) {
				target.layoutXProperty().addListener((observable, oldValue, newValue) -> updateEdge.run());

				target.layoutYProperty().addListener((observable, oldValue, newValue) -> updateEdge.run());
			}

			Tooltip tooltip = new Tooltip(description);

			tooltip.setShowDelay(javafx.util.Duration.millis(150));
			tooltip.setHideDelay(javafx.util.Duration.millis(100));

			Tooltip.install(hoverCurve, tooltip);
			Tooltip.install(curve, tooltip);
			Tooltip.install(arrow, tooltip);

			configureHover();
			configureEdgeDragging(source, target, onSelected);

			Platform.runLater(updateEdge);
		}

		public String getEdgeId() {
			return edgeId;
		}

		@Override
		public void setSelected(boolean selected) {
			this.selected = selected;
			updateEdgeStyle();
		}

		private void updateEdgeStyle() {
			if (selected) {
				curve.setStroke(Color.web("#d32f2f"));
				curve.setStrokeWidth(3.5);
				arrow.setFill(Color.web("#d32f2f"));
			} else {
				curve.setStroke(NORMAL_COLOR);
				curve.setStrokeWidth(1.8);
				arrow.setFill(NORMAL_COLOR);
			}
		}

		public void resetShape() {
			userOffset = 0.0;
			loopOffsetX = 0.0;
			loopOffsetY = 0.0;
		}

		private void updateEdgeLabel() {
			double t = 0.5;

			double x = cubicValue(curve.getStartX(), curve.getControlX1(), curve.getControlX2(), curve.getEndX(), t);

			double y = cubicValue(curve.getStartY(), curve.getControlY1(), curve.getControlY2(), curve.getEndY(), t);

			edgeLabel.applyCss();
			edgeLabel.autosize();

			edgeLabel.setLayoutX(x - edgeLabel.getWidth() / 2.0);

			edgeLabel.setLayoutY(y - edgeLabel.getHeight() - 8.0);
		}

		private double cubicValue(double start, double control1, double control2, double end, double t) {
			double inverseT = 1.0 - t;

			return inverseT * inverseT * inverseT * start + 3.0 * inverseT * inverseT * t * control1
					+ 3.0 * inverseT * t * t * control2 + t * t * t * end;
		}

		private void configureEdgeDragging(StateView source, StateView target, Runnable onSelected) {

			hoverCurve.setOnMouseClicked(event -> {
				if (event.getClickCount() == 1) {
					onSelected.run();
					event.consume();
					return;
				}

				if (event.getClickCount() == 2) {
					userOffset = 0.0;
					loopOffsetX = 0.0;
					loopOffsetY = 0.0;

					if (source == target) {
						updateLoop(source);
					} else {
						updateNormalEdge(source, target);
					}

					updateArrowHead();
					onSelected.run();

					event.consume();
				}
			});
			hoverCurve.setOnMousePressed(event -> {
				if (!event.isPrimaryButtonDown()) {
					return;
				}

				hoverCurve.setStyle("-fx-cursor: closed-hand;");

				dragStartPoint = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());

				dragStartOffset = userOffset;

				dragStartLoopOffsetX = loopOffsetX;
				dragStartLoopOffsetY = loopOffsetY;

				event.consume();
			});

			hoverCurve.setOnMouseDragged(event -> {
				if (!event.isPrimaryButtonDown()) {
					return;
				}

				if (dragStartPoint == null) {
					return;
				}

				Point2D currentPoint = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());

				double movementX = currentPoint.getX() - dragStartPoint.getX();

				double movementY = currentPoint.getY() - dragStartPoint.getY();

				if (source == target) {
					loopOffsetX = dragStartLoopOffsetX + movementX;

					loopOffsetY = dragStartLoopOffsetY + movementY;

					updateLoop(source);
				} else {
					updateNormalEdgeDragging(source, target, movementX, movementY);
				}

				updateArrowHead();

				event.consume();
			});

			hoverCurve.setOnMouseReleased(event -> {
				dragStartPoint = null;
				hoverCurve.setStyle("-fx-cursor: hand;");
				event.consume();
			});
		}

		private void updateNormalEdgeDragging(StateView source, StateView target, double movementX, double movementY) {
			double edgeX = target.getCenterX() - source.getCenterX();

			double edgeY = target.getCenterY() - source.getCenterY();

			double edgeLength = Math.hypot(edgeX, edgeY);

			if (edgeLength == 0.0) {
				return;
			}

			double perpendicularX = -edgeY / edgeLength;

			double perpendicularY = edgeX / edgeLength;

			double perpendicularMovement = movementX * perpendicularX + movementY * perpendicularY;

			if (source.getStateId().compareTo(target.getStateId()) > 0) {
				perpendicularMovement = -perpendicularMovement;
			}

			userOffset = dragStartOffset + perpendicularMovement;

			updateNormalEdge(source, target);
		}

		private void updateNormalEdge(StateView source, StateView target) {
			Point2D start = calculateStart(source, target);
			Point2D end = calculateEnd(source, target);

			double dx = end.getX() - start.getX();
			double dy = end.getY() - start.getY();
			double distance = Math.hypot(dx, dy);

			double middleX = (start.getX() + end.getX()) / 2.0;
			double middleY = (start.getY() + end.getY()) / 2.0;

			double centeredIndex = parallelIndex - (parallelCount - 1) / 2.0;

			double offset = centeredIndex * PARALLEL_SPACING + userOffset;

			if (source.getStateId().compareTo(target.getStateId()) > 0) {
				offset = -offset;
			}

			double controlX = middleX;
			double controlY = middleY;

			if (distance > 0.0) {
				double perpendicularX = -dy / distance;
				double perpendicularY = dx / distance;

				controlX += perpendicularX * offset;
				controlY += perpendicularY * offset;
			}

			curve.setStartX(start.getX());
			curve.setStartY(start.getY());

			curve.setControlX1(controlX);
			curve.setControlY1(controlY);

			curve.setControlX2(controlX);
			curve.setControlY2(controlY);

			curve.setEndX(end.getX());
			curve.setEndY(end.getY());
		}

		private void updateLoop(StateView state) {
			double centerX = state.getCenterX();
			double centerY = state.getCenterY();
			double radius = state.getRadius();

			double automaticLoopOffset = parallelIndex * LOOP_SPACING;

			double loopWidth = LOOP_WIDTH + automaticLoopOffset * 0.6 + Math.abs(loopOffsetX) * 0.25;

			double loopHeight = LOOP_HEIGHT + automaticLoopOffset - loopOffsetY;

			/*
			 * Evita que el bucle se haga demasiado pequeño al arrastrarlo hacia abajo.
			 */
			loopHeight = Math.max(radius + 25.0, loopHeight);

			double connectionFactor = Math.min(0.65 + parallelIndex * 0.08, 0.9);

			double startX = centerX - radius * connectionFactor + loopOffsetX;

			double startY = centerY - radius * 0.75;

			double endX = centerX + radius * connectionFactor + loopOffsetX;

			double endY = centerY - radius * 0.75;

			curve.setStartX(startX);
			curve.setStartY(startY);

			curve.setControlX1(centerX - loopWidth + loopOffsetX);

			curve.setControlY1(centerY - loopHeight);

			curve.setControlX2(centerX + loopWidth + loopOffsetX);

			curve.setControlY2(centerY - loopHeight);

			curve.setEndX(endX);
			curve.setEndY(endY);
		}

		private Point2D calculateStart(StateView source, StateView target) {
			return pointOnCircle(source.getCenterX(), source.getCenterY(), target.getCenterX(), target.getCenterY(),
					source.getRadius());
		}

		private Point2D calculateEnd(StateView source, StateView target) {
			return pointOnCircle(target.getCenterX(), target.getCenterY(), source.getCenterX(), source.getCenterY(),
					target.getRadius());
		}

		private Point2D pointOnCircle(double centerX, double centerY, double targetX, double targetY, double radius) {
			double dx = targetX - centerX;
			double dy = targetY - centerY;
			double distance = Math.hypot(dx, dy);

			if (distance == 0.0) {
				return new Point2D(centerX, centerY);
			}

			return new Point2D(centerX + dx / distance * radius, centerY + dy / distance * radius);
		}

		private void updateArrowHead() {
			double endX = curve.getEndX();
			double endY = curve.getEndY();

			double controlX = curve.getControlX2();
			double controlY = curve.getControlY2();

			double angle = Math.atan2(endY - controlY, endX - controlX);

			double sin = Math.sin(angle);
			double cos = Math.cos(angle);

			double x1 = endX - ARROW_SIZE * cos + ARROW_SIZE * 0.55 * sin;

			double y1 = endY - ARROW_SIZE * sin - ARROW_SIZE * 0.55 * cos;

			double x2 = endX - ARROW_SIZE * cos - ARROW_SIZE * 0.55 * sin;

			double y2 = endY - ARROW_SIZE * sin + ARROW_SIZE * 0.55 * cos;

			arrow.getPoints().setAll(endX, endY, x1, y1, x2, y2);
		}

		private void configureHover() {
			hoverCurve.setOnMouseEntered(event -> setHighlighted(true));

			hoverCurve.setOnMouseExited(event -> setHighlighted(false));

			curve.setOnMouseEntered(event -> setHighlighted(true));

			curve.setOnMouseExited(event -> setHighlighted(false));

			arrow.setOnMouseEntered(event -> setHighlighted(true));

			arrow.setOnMouseExited(event -> setHighlighted(false));
		}

		private void setHighlighted(boolean highlighted) {
			if (selected) {
				updateEdgeStyle();
				return;
			}

			if (highlighted) {
				curve.setStroke(HOVER_COLOR);
				curve.setStrokeWidth(3.5);
				arrow.setFill(HOVER_COLOR);
			} else {
				curve.setStroke(NORMAL_COLOR);
				curve.setStrokeWidth(1.8);
				arrow.setFill(NORMAL_COLOR);
			}
		}
	}

	public record StateModel(String id, String label, double x, double y, boolean initial) {
	}

	public record EdgeModel(String id, String sourceId, String targetId, String label, String description) {
	}

	public record GraphModel(List<StateModel> states, List<EdgeModel> edges) {
	}
}