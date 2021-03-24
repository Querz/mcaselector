package net.querz.mcaselector.io.db;

import net.querz.mcaselector.debug.Debug;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.tiles.overlay.OverlayParser;
import net.querz.mcaselector.validation.ShutdownHooks;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class CacheDBController {

	private Connection connection;
	private String dbPath;
	private ShutdownHooks.ShutdownJob closeShutdownHook;
	private List<String> allTables;

	static {
		try {
			new org.sqlite.JDBC();
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException ex) {
			System.err.println("failed to load jdbc driver");
			ex.printStackTrace();
			System.exit(0);
		}
	}

	public void switchTo(String dbPath, List<OverlayParser> parsers) throws SQLException {
		removeCloseShutdownHook();
		close();

		File dbFile = new File(dbPath);

		if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
			dbFile.getParentFile().mkdirs();
		}

		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
		} catch (SQLException ex) {
			Debug.dumpException("failed to open cache db", ex);
			Debug.dump("attempting to create new cache db");

			if (new File(dbPath).delete()) {
				Debug.dump("successfully deleted corrupted cache db");
				connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
			} else {
				Debug.dump("failed to delete corrupted cache db");
				throw new SQLException("failed to delete corrupted cache db");
			}
		}

		this.dbPath = dbPath;
		addCloseShutdownHook();

		initTables(parsers);
	}

	public void initTables(List<OverlayParser> parsers) throws SQLException {
		Statement statement = connection.createStatement();
		for (OverlayParser parser : parsers) {

			if (parser.multiValues() == null) {
				statement.executeUpdate(String.format(
						"CREATE TABLE IF NOT EXISTS %s (" +
								"p BIGINT PRIMARY KEY, " +
								"d BLOB);", parser.name()));
			} else {
				for (String suffix : parser.multiValues()) {
					statement.executeUpdate(String.format(
							"CREATE TABLE IF NOT EXISTS %s_%s (" +
									"p BIGINT PRIMARY KEY, " +
									"d BLOB);", parser.name(), suffix));
				}
			}
		}

		allTables = new ArrayList<>();
		ResultSet result = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");
		while (result.next()) {
			allTables.add(result.getString(1));
		}
	}

	public void close() throws SQLException {
		if (connection != null && !connection.isClosed()) {
			connection.close();
			if (connection.isClosed()) {
				Debug.dump("cache db connection closed");
			} else {
				Debug.dump("failed to close cache db connection");
			}
			dbPath = null;
			connection = null;
		}
	}

	private void closeOnShutdown() {
		try {
			close();
		} catch (SQLException ex) {
			Debug.dumpException("failed to close cache db connection with exception", ex);
		}
	}

	public void addCloseShutdownHook() {
		if (closeShutdownHook == null) {
			closeShutdownHook = ShutdownHooks.addShutdownHook(this::closeOnShutdown, 100);
		} else {
			throw new RuntimeException("attempted to add a shutdown hook for db connection while one was already present");
		}
	}

	public void removeCloseShutdownHook() {
		if (closeShutdownHook != null) {
			ShutdownHooks.removeShutdownHook(closeShutdownHook);
			closeShutdownHook = null;
		}
	}

	public int[] getData(OverlayParser parser, String suffix, Point2i region) throws IOException, SQLException {
		Statement statement = connection.createStatement();
		ResultSet result = statement.executeQuery(String.format(
				"SELECT d FROM %s WHERE p=%s;", parser.name() + (suffix == null ? "" : "_" + suffix), region.asLong()));
		if (!result.next()) {
			return null;
		}
		int[] data = new int[1024];
		try (DataInputStream dis = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(result.getBytes(1))))) {
			for (int i = 0; i < 1024; i++) {
				data[i] = dis.readInt();
			}
		}
		return data;
	}

	public void setData(OverlayParser parser, String suffix, Point2i region, int[] data) throws IOException, SQLException {
		PreparedStatement ps = connection.prepareStatement(String.format(
				"INSERT INTO %s (p, d) " +
						"VALUES (?, ?) " +
						"ON CONFLICT(p) DO UPDATE " +
						"SET d=?;", parser.name() + (suffix == null ? "" : "_" + suffix)));
		ps.setLong(1, region.asLong());
		ByteArrayOutputStream baos;
		try (DataOutputStream dos = new DataOutputStream(new GZIPOutputStream(baos = new ByteArrayOutputStream()))) {
			for (int i = 0; i < 1024; i++) {
				dos.writeInt(data[i]);
			}
		}
		byte[] gzipped = baos.toByteArray();
		ps.setBytes(2, gzipped);
		ps.setBytes(3, gzipped);
		ps.addBatch();
		ps.executeBatch();
	}

	public void deleteData(OverlayParser parser, String suffix, Point2i region) throws SQLException {
		PreparedStatement ps = connection.prepareStatement(String.format(
				"DELETE FROM %s WHERE p=?;", parser.name() + (suffix == null ? "" : "_" + suffix)));
		ps.setLong(1, region.asLong());
		ps.execute();
	}

	public void deleteData(Point2i region) throws SQLException {
		for (String table : allTables) {
			PreparedStatement ps = connection.prepareStatement(String.format(
					"DELETE FROM %s WHERE p=?;", table));
			ps.setLong(1, region.asLong());
			ps.execute();
		}
	}

	public void clear(List<OverlayParser> parsers) throws IOException, SQLException {
		if (dbPath == null) {
			return;
		}
		File dbFile = new File(this.dbPath);
		close();
		if (dbFile.delete()) {
			Debug.dumpf("deleted cache db %s", dbFile);
		} else {
			throw new IOException(String.format("failed to delete cache db %s", dbFile.getCanonicalPath()));
		}
		switchTo(dbFile.getPath(), parsers);
	}
}
