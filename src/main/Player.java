package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;

/** Example of playing all audio files in a given directory. */
public class Player extends Application {

	public static final String TAG_COLUMN_NAME = "Tag";
	public static final String VALUE_COLUMN_NAME = "Value";
	public static final List<String> SUPPORTED_FILE_EXTENSIONS = Arrays.asList(".mp3", ".m4a");
	public static final int FILE_EXTENSION_LEN = 3;
	public static final String DIR_PATH = "src/temp/dir.txt";

	final TextFlow currentlyPlaying = new TextFlow();
	@SuppressWarnings("rawtypes")
	final TableView<Map> metadataTable = new TableView<>();
	private ChangeListener<Duration> progressChangeListener;
	private MapChangeListener<String, Object> metadataChangeListener;

	final Image iskip = new Image("/img/skip.png", 20, 20, true, false);
	final Image ipause = new Image("/img/pause.png", 30, 30, true, false);
	final Image iplay = new Image("/img/play.png", 30, 30, true, false);
	final Image iexit = new Image("img/exit.png", 20, 20, true, false);
	final Image iset = new Image("img/settings.png", 20, 20, true, false);
	final Image isound = new Image("/img/sound.png", 30, 30, true, false);
	final Image imute = new Image("/img/mute.png", 30, 30, true, false);

	final ImageView skip = new ImageView(iskip);
	final ImageView play = new ImageView(ipause);
	final ImageView exit = new ImageView(iexit);
	final ImageView settings = new ImageView(iset);
	final ImageView sound = new ImageView(isound);
	final ImageView mute = new ImageView(imute);

	Stage mainStage;
	String musdir = "C:/";
	Text text = new Text();
	ProgressBar progress = new ProgressBar();
	Slider vol = new Slider(0, 0.2, 0.1);
	MediaView mediaView = new MediaView();

	private void chooseDir(int i) {
		DirectoryChooser dc = new DirectoryChooser();
		dc.setTitle("Choose your Music Directory :");
		dc.setInitialDirectory(new File(musdir));
		String dir = dc.showDialog(mainStage).toString();
		if (dir != null) {
			setMusDir(dir);
			musdir = getMusDir();
			currentlyPlaying.setBackground(null);
			if (mediaView.getMediaPlayer() != null)
				mediaView.getMediaPlayer().stop();
			if (i == 0)
				initPlayer();
		}
	}

	@SuppressWarnings("rawtypes")
	private ObservableList<Map> convertMetadataToTableData(ObservableMap<String, Object> metadata) {
		ObservableList<Map> allData = FXCollections.observableArrayList();

		for (String key : metadata.keySet()) {
			Map<String, Object> dataRow = new HashMap<>();

			dataRow.put(TAG_COLUMN_NAME, key);
			dataRow.put(VALUE_COLUMN_NAME, metadata.get(key));

			allData.add(dataRow);
		}

		return allData;
	}

	/**
	 * @return a MediaPlayer for the given source which will report any errors
	 *         it encounters
	 */
	private MediaPlayer createPlayer(String mediaSource) {
		final Media media = new Media(mediaSource);
		final MediaPlayer player = new MediaPlayer(media);
		player.setOnError(new Runnable() {
			@Override
			public void run() {
				System.out.println("Media error occurred: " + player.getError());
			}
		});
		return player;
	}

	private String getMusDir() {
		try {
			File path = new File(DIR_PATH.substring(0, DIR_PATH.length()-8));
			path.mkdirs();
			File file = new File(DIR_PATH);
			if(file.createNewFile()){
				PrintWriter writer = new PrintWriter(new FileWriter(DIR_PATH));
				writer.write("C:/");
				writer.close();
			}
			BufferedReader reader = new BufferedReader(new FileReader(DIR_PATH));
			String dir = reader.readLine();
			reader.close();
			return dir;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void initPlayer() {
		skip.setPickOnBounds(true);
		play.setPickOnBounds(true);
		exit.setPickOnBounds(true);
		settings.setPickOnBounds(true);
		// determine the source directory for the playlist (either the first
		// argument to the program or a default).
		final List<String> params = getParameters().getRaw();
		final File dir = (params.size() > 0) ? new File(params.get(0)) : new File(musdir);
		if (!dir.exists() || !dir.isDirectory()) {
			chooseDir(0);
			return;
		}

		// create some media players.
		final List<MediaPlayer> players = new ArrayList<>();
		for (String file : dir.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				for (String ext : SUPPORTED_FILE_EXTENSIONS) {
					if (name.endsWith(ext)) {
						return true;
					}
				}

				return false;
			}
		}))
			players.add(createPlayer("file:///" + (dir + "\\" + file).replace("\\", "/").replaceAll(" ", "%20")));
		if (players.isEmpty()) {
			chooseDir(0);
		}

		// create a view to show the mediaplayers.
		if(players.isEmpty()){
			musdir = getMusDir();
			initPlayer();
		}else{
			mediaView.setMediaPlayer(players.get(0));
		}
		
		// play each audio file in turn.
		for (int i = 0; i < players.size(); i++) {
			final MediaPlayer player = players.get(i);
			final MediaPlayer nextPlayer = players.get((i + 1) % players.size());
			players.get(i).setVolume(0.1);
			player.setOnEndOfMedia(new Runnable() {
				@Override
				public void run() {
					Double vol = player.getVolume();
					player.currentTimeProperty().removeListener(progressChangeListener);
					player.getMedia().getMetadata().removeListener(metadataChangeListener);
					player.stop();
					mediaView.setMediaPlayer(nextPlayer);
					nextPlayer.setVolume(vol);
					nextPlayer.play();
				}
			});
		}

		// allow the user to skip a track.
		skip.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				final MediaPlayer curPlayer = mediaView.getMediaPlayer();
				Double vol = curPlayer.getVolume();
				curPlayer.currentTimeProperty().removeListener(progressChangeListener);
				curPlayer.getMedia().getMetadata().removeListener(metadataChangeListener);
				curPlayer.stop();

				MediaPlayer nextPlayer = players.get((players.indexOf(curPlayer) + 1) % players.size());
				mediaView.setMediaPlayer(nextPlayer);
				nextPlayer.setVolume(vol);
				nextPlayer.play();
				play.setImage(ipause);
			}
		});

		// allow the user to play or pause a track.
		play.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if (ipause.equals(play.getImage())) {
					mediaView.getMediaPlayer().pause();
					play.setImage(iplay);
				} else {
					mediaView.getMediaPlayer().play();
					play.setImage(ipause);
				}
			}
		});

		// display the name of the currently playing track.
		mediaView.mediaPlayerProperty().addListener(new ChangeListener<MediaPlayer>() {
			@Override
			public void changed(ObservableValue<? extends MediaPlayer> observableValue, MediaPlayer oldPlayer,
					MediaPlayer newPlayer) {
				setCurrentlyPlaying(newPlayer);
			}
		});

		// start playing the first track.
		mediaView.setMediaPlayer(players.get(0));
		mediaView.getMediaPlayer().play();
		setCurrentlyPlaying(mediaView.getMediaPlayer());

		// silly invisible button used as a template to get the actual preferred
		// size of the Pause button.
		Button invisiblePause = new Button("Pause");
		invisiblePause.setPrefWidth(30);
		invisiblePause.setPrefHeight(30);
		invisiblePause.setVisible(false);

		// add a metadataTable for meta data display
		metadataTable.setStyle("-fx-font-size: 13px;");

		TableColumn<Map, String> tagColumn = new TableColumn<>(TAG_COLUMN_NAME);
		tagColumn.setPrefWidth(150);
		TableColumn<Map, Object> valueColumn = new TableColumn<>(VALUE_COLUMN_NAME);
		valueColumn.setPrefWidth(400);

		tagColumn.setCellValueFactory(new MapValueFactory<>(TAG_COLUMN_NAME));
		valueColumn.setCellValueFactory(new MapValueFactory<>(VALUE_COLUMN_NAME));

		metadataTable.setEditable(true);
		metadataTable.getSelectionModel().setCellSelectionEnabled(true);
		metadataTable.getColumns().setAll(tagColumn, valueColumn);
		valueColumn.setCellFactory(new Callback<TableColumn<Map, Object>, TableCell<Map, Object>>() {
			@Override
			public TableCell<Map, Object> call(TableColumn<Map, Object> column) {
				return new TableCell<Map, Object>() {
					@Override
					protected void updateItem(Object item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							if (item instanceof String) {
								setText((String) item);
								setGraphic(null);
							} else if (item instanceof Integer) {
								setText(Integer.toString((Integer) item));
								setGraphic(null);
							} else if (item instanceof Image) {
								setText(null);
								ImageView imageView = new ImageView((Image) item);
								imageView.setFitWidth(200);
								imageView.setPreserveRatio(true);
								setGraphic(imageView);
							} else {
								setText("N/A");
								setGraphic(null);
							}
						} else {
							setText(null);
							setGraphic(null);
						}
					}
				};
			}
		});
		mediaView.getMediaPlayer().setVolume(vol.getValue());
		
		Delta dragDelta = new Delta();
		// layout the scene.
		VBox layout = new VBox(10);
		HBox icons = new HBox(10);
		HBox volume = new HBox(10);
		icons.setAlignment(Pos.CENTER);

		HBox progressReport = new HBox(5);
		progressReport.setAlignment(Pos.CENTER);
		progressReport.getChildren().setAll(play, progress, skip, mediaView);

		HBox title = new HBox(50);
		title.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if (e.getButton().equals(MouseButton.PRIMARY)) {
					dragDelta.x = mainStage.getX() - e.getScreenX();
					dragDelta.y = mainStage.getY() - e.getScreenY();
				}
			}
		});
		title.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent e) {
				if (e.getButton().equals(MouseButton.PRIMARY)) {
					mainStage.setX(e.getScreenX() + dragDelta.x);
					mainStage.setY(e.getScreenY() + dragDelta.y);
				}
			}
		});
		Label t = new Label();
		t.setText("Gaming Music Player");
		exit.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				System.exit(0);
			}
		});
		settings.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				chooseDir(0);
			}
		});
		vol.valueProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number oldv, Number newv) {
				mediaView.getMediaPlayer().setVolume(newv.doubleValue());
			}
		});

		volume.getChildren().addAll(mute, vol, sound);
		volume.setAlignment(Pos.CENTER);

		icons.getChildren().addAll(settings, exit);
		title.setStyle(
				"-fx-alignment:center;" + "-fx-font-size:20px;" + "-fx-background-color: rgba(255,255,255,0.5);");
		title.getChildren().setAll(t, icons);

		VBox content = new VBox(10);
		progressReport.setPadding(new Insets(0, 10, 0, 10));
		content.setAlignment(Pos.CENTER);
		content.getChildren().setAll(title, currentlyPlaying, progressReport, volume);

		layout.getChildren().addAll(invisiblePause, content);
		progress.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(progress, Priority.ALWAYS);
		VBox.setVgrow(metadataTable, Priority.ALWAYS);

		layout.setStyle("-fx-effect: dropshadow(gaussian, grey, 50, 0, 0, 0);" + "-fx-background-insets: 50; "
				+ "-fx-background-color: rgba(255,255,255,0.5);" + "-fx-padding:0 50 50 50;" + "-fx-text-fill:grey;");
		layout.setAlignment(Pos.CENTER);

		layout.setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				title.setOpacity(1);
				progressReport.setOpacity(1);
				volume.setOpacity(1);
				layout.setStyle("-fx-effect: dropshadow(gaussian, grey, 50, 0, 0, 0);" + "-fx-background-insets: 50; "
						+ "-fx-background-color: rgba(255,255,255,0.5);" + "-fx-padding:0 50 50 50;"
						+ "-fx-text-fill:grey;" + "-fx-alignment: center;");
				currentlyPlaying.setBackground(null);
			}
		});

		layout.setOnMouseExited(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				title.setOpacity(0);
				progressReport.setOpacity(0);
				volume.setOpacity(0);
				layout.setStyle("-fx-effect: dropshadow(gaussian, grey, 50, 0, 0, 0);" + "-fx-background-insets: 50; "
						+ "-fx-background-color: rgba(255,255,255,0);" + "-fx-padding:0 50 50 50;"
						+ "-fx-text-fill:grey;" + "-fx-alignment: center;");
				currentlyPlaying.setBackground(new Background(
						new BackgroundFill(Color.rgb(200, 200, 200, 0.5), new CornerRadii(10), new Insets(-5))));
			}
		});
		Scene scene = new Scene(layout, 400, 250);
		scene.setFill(Color.TRANSPARENT);
		mainStage.setScene(scene);
		mainStage.show();
	}

	/**
	 * sets the currently playing label to the label of the new media player and
	 * updates the progress monitor.
	 */

	public static void main(String[] args) {
		launch(args);
	}

	private void setCurrentlyPlaying(final MediaPlayer newPlayer) {
		newPlayer.seek(Duration.ZERO);

		progress.setProgress(0);
		progressChangeListener = new ChangeListener<Duration>() {
			@Override
			public void changed(ObservableValue<? extends Duration> observableValue, Duration oldValue,
					Duration newValue) {
				progress.setProgress(
						1.0 * newPlayer.getCurrentTime().toMillis() / newPlayer.getTotalDuration().toMillis());
			}
		};
		newPlayer.currentTimeProperty().addListener(progressChangeListener);

		String source = newPlayer.getMedia().getSource();
		source = source.substring(0, source.length() - FILE_EXTENSION_LEN - 1);
		source = source.substring(source.lastIndexOf("/") + 1).replaceAll("%20", " ");
		text.setText(source);
		text.setFont(Font.font("Calibri", FontWeight.BLACK, 20));
		if (!currentlyPlaying.getChildren().isEmpty())
			currentlyPlaying.getChildren().remove(0);
		currentlyPlaying.getChildren().add(text);
		currentlyPlaying.setTextAlignment(TextAlignment.CENTER);
		setMetaDataDisplay(newPlayer.getMedia().getMetadata());
	}

	private void setMetaDataDisplay(ObservableMap<String, Object> metadata) {
		metadataTable.getItems().setAll(convertMetadataToTableData(metadata));
		metadataChangeListener = new MapChangeListener<String, Object>() {
			@Override
			public void onChanged(Change<? extends String, ?> change) {
				metadataTable.getItems().setAll(convertMetadataToTableData(metadata));
			}
		};
		metadata.addListener(metadataChangeListener);
	}

	private void setMusDir(String dir) {
		try {
			PrintWriter writer;
			writer = new PrintWriter(new FileWriter(DIR_PATH));
			writer.println(dir);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start(final Stage stage) {
		stage.initStyle(StageStyle.TRANSPARENT);
		stage.setAlwaysOnTop(true);
		mainStage = stage;
		musdir = getMusDir();
		initPlayer();
	}
}