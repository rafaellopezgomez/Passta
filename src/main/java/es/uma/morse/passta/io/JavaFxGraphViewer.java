package es.uma.morse.passta.io;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.scene.shape.CubicCurve;

public class JavaFxGraphViewer extends Application {

	private static GraphModel graphToShow;

	private final Pane graphPane = new Pane();
	private final Group graphGroup = new Group(graphPane);

	private double panStartX;
	private double panStartY;
	private double groupStartX;
	private double groupStartY;

	public static void open(GraphModel graph) {
		graphToShow = graph;

		Thread thread = new Thread(() -> Application.launch(JavaFxGraphViewer.class));
		thread.setName("passta-javafx-viewer");
		thread.setDaemon(false);
		thread.start();
	}

	@Override
	public void start(Stage stage) {
		graphPane.setPrefSize(1600, 1000);
		graphPane.setStyle("-fx-background-color: #fafafa;");

		Pane root = new Pane(graphGroup);

		createGraph(graphToShow);
		configureZoom(root);
		configurePan(root);

		Scene scene = new Scene(root, 1200, 800);

		stage.setTitle("PASSTA Graph Viewer");
		stage.setScene(scene);
		stage.show();
	}

	private void createGraph(GraphModel graph) {
		Map<String, StateView> stateViews = new HashMap<>();

		for (StateModel state : graph.states()) {
			StateView view = new StateView(state.id(), state.label(), state.x(), state.y());

			stateViews.put(state.id(), view);
		}

		Map<String, List<EdgeModel>> edgeGroups = new LinkedHashMap<>();

		for (EdgeModel edge : graph.edges()) {
			String groupKey = edge.sourceId() + "->" + edge.targetId();

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

				EdgeView edgeView = new EdgeView(edge.id(), source, target, edge.description(), parallelIndex,
						parallelCount);

				graphPane.getChildren().add(edgeView);
			}
		}

		graphPane.getChildren().addAll(stateViews.values());
	}

	private StateView findState(List<StateView> states, String id) {
		return states.stream().filter(state -> state.getStateId().equals(id)).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Estado no encontrado: " + id));
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

			/*
			 * Solo desplazamos el lienzo cuando se pulsa directamente sobre el fondo.
			 */
			if (event.getTarget() != root && event.getTarget() != graphPane) {
				return;
			}

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

		root.setOnMouseReleased(event -> root.setStyle("-fx-cursor: default;"));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	public static final class StateView extends StackPane {

		private static final double RADIUS = 28.0;

		private final String stateId;

		private double dragOffsetX;
		private double dragOffsetY;

		public StateView(String stateId, String labelText, double x, double y) {
			this.stateId = stateId;

			Circle circle = new Circle(RADIUS);
			circle.setFill(Color.WHITE);
			circle.setStroke(Color.web("#2f4f6f"));
			circle.setStrokeWidth(2.0);

			Label label = new Label(labelText);
			label.setMouseTransparent(true);
			label.setWrapText(true);
			label.setMaxWidth(140.0);
			label.setStyle("-fx-alignment: center;" + "-fx-text-alignment: center;");

			getChildren().addAll(circle, label);

			setLayoutX(x - RADIUS);
			setLayoutY(y - RADIUS);

			setStyle("-fx-cursor: hand;");

			configureDragging();
		}

		public String getStateId() {
			return stateId;
		}

		public double getCenterX() {
			return getLayoutX() + RADIUS;
		}

		public double getCenterY() {
			return getLayoutY() + RADIUS;
		}

		public double getRadius() {
			return RADIUS;
		}

		private void configureDragging() {
			setOnMousePressed(event -> {
				if (event.getButton() != MouseButton.PRIMARY) {
					return;
				}

				Point2D parentPoint = getParent().sceneToLocal(event.getSceneX(), event.getSceneY());

				dragOffsetX = parentPoint.getX() - getLayoutX();
				dragOffsetY = parentPoint.getY() - getLayoutY();

				toFront();
				setStyle("-fx-cursor: closed-hand;");

				event.consume();
			});

			setOnMouseDragged(event -> {
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

	public static final class EdgeView extends Group {

		private static final double ARROW_SIZE = 10.0;
		private static final double LOOP_HEIGHT = 90.0;
		private static final double LOOP_WIDTH = 55.0;

		private static final Color NORMAL_COLOR = Color.web("#555555");
		private static final Color HOVER_COLOR = Color.web("#1976d2");

		private final CubicCurve curve = new CubicCurve();
		private final Polygon arrow = new Polygon();
		private final String edgeId;

		private final int parallelIndex;
		private final int parallelCount;

		private static final double PARALLEL_SPACING = 45.0;
		private static final double LOOP_SPACING = 25.0;

		public EdgeView(String edgeId, StateView source, StateView target, String description, int parallelIndex,
				int parallelCount) {
			this.edgeId = edgeId;
			this.parallelIndex = parallelIndex;
			this.parallelCount = parallelCount;

			curve.setFill(Color.TRANSPARENT);
			curve.setStroke(NORMAL_COLOR);
			curve.setStrokeWidth(1.8);

			arrow.setFill(NORMAL_COLOR);

			getChildren().addAll(curve, arrow);

			Runnable updateEdge = () -> {
				if (source == target) {
					updateLoop(source);
				} else {
					updateNormalEdge(source, target);
				}

				updateArrowHead();
			};

			source.layoutXProperty().addListener((observable, oldValue, newValue) -> updateEdge.run());

			source.layoutYProperty().addListener((observable, oldValue, newValue) -> updateEdge.run());

			if (source != target) {
				target.layoutXProperty().addListener((observable, oldValue, newValue) -> updateEdge.run());

				target.layoutYProperty().addListener((observable, oldValue, newValue) -> updateEdge.run());
			}

			Tooltip tooltip = new Tooltip(description);

			Tooltip.install(curve, tooltip);
			Tooltip.install(arrow, tooltip);

			configureHover();

			Platform.runLater(updateEdge);
		}

		public String getEdgeId() {
			return edgeId;
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

			double offset = centeredIndex * PARALLEL_SPACING;

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

			double loopOffset = parallelIndex * LOOP_SPACING;

			double loopWidth = LOOP_WIDTH + loopOffset * 0.6;
			double loopHeight = LOOP_HEIGHT + loopOffset;

			double connectionFactor = Math.min(0.65 + parallelIndex * 0.08, 0.9);

			double startX = centerX - radius * connectionFactor;
			double startY = centerY - radius * 0.75;

			double endX = centerX + radius * connectionFactor;
			double endY = centerY - radius * 0.75;

			curve.setStartX(startX);
			curve.setStartY(startY);

			curve.setControlX1(centerX - loopWidth);
			curve.setControlY1(centerY - loopHeight);

			curve.setControlX2(centerX + loopWidth);
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

			/*
			 * La dirección de la flecha se obtiene usando la tangente formada por el
			 * segundo punto de control y el punto final.
			 */
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
			curve.setOnMouseEntered(event -> setHighlighted(true));
			curve.setOnMouseExited(event -> setHighlighted(false));

			arrow.setOnMouseEntered(event -> setHighlighted(true));
			arrow.setOnMouseExited(event -> setHighlighted(false));
		}

		private void setHighlighted(boolean highlighted) {
			Color color = highlighted ? HOVER_COLOR : NORMAL_COLOR;

			curve.setStroke(color);
			arrow.setFill(color);
			curve.setStrokeWidth(highlighted ? 3.5 : 1.8);
		}

	}

	public record StateModel(String id, String label, double x, double y) {
	}

	public record EdgeModel(String id, String sourceId, String targetId, String description) {
	}

	public record GraphModel(List<StateModel> states, List<EdgeModel> edges) {
	}
}