package metric.correlation.analysis.database;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sample;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.gravity.eclipse.os.Execute;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoSocketReadException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;

/**
 * contains helper methods for dealing with the mongoDB and metrics
 * [TODO:] richtiges ORM verwenden
 *
 * @author Stefan Thießen
 *
 */
public class MongoDBHelper implements AutoCloseable {

	private static final String CLASS = "class";

	private static final Logger LOGGER = Logger.getLogger(MongoDBHelper.class);

	public static final int DEFAULT_PORT = 27017;
	public static final String SERVER_URI = "mongodb://localhost:" + DEFAULT_PORT;
	public static final String DEFAULT_DATABASE = "metric_correlation";
	public static final String DEFAULT_COLLECTION = "metrics";
	public static final String CLASS_COLLECTION = "class-metrics";
	public static final String MONGOD = "MONGOD";
	// keys to identify a projects metric results
	private final List<String> projectKeys = Arrays.asList("version", "product", "vendor");

	private MongoCollection<Document> dbCollection;
	private final MongoClient client;
	private final String databaseName;
	private final String collectionName;
	static Process mongodProcess;

	public MongoDBHelper() {
		this(DEFAULT_DATABASE, DEFAULT_COLLECTION);
	}

	public MongoDBHelper(final String databaseName, final String collectionName) {
		this.databaseName = databaseName;
		this.collectionName = collectionName;
		this.client = new MongoClient(new MongoClientURI(SERVER_URI));
		this.dbCollection = this.client.getDatabase(databaseName).getCollection(collectionName);
	}

	/**
	 * starts the server using the .cfg file in the mongodb folder, if present. Better start manually and not use this
	 */
	public static void startServer() {
		if (available(DEFAULT_PORT)) {
			final String config = findConfig();
			final List<String> args = config == null ? null : Arrays.asList("--config", config);
			try {
				mongodProcess = Execute.run(getMongoDBFolder(), "mongod", args, null);
				Thread.sleep(5000); // give the server time to start
				if (available(DEFAULT_PORT)) {
					final String msg = Execute.collectMessages(mongodProcess).toString();
					LOGGER.warn(msg);
					throw new IllegalStateException();
				}
			} catch (final Exception e) {
				LOGGER.log(Level.ERROR, "COULD NOT START THE MONGODB SERVER");
				LOGGER.error(e.getStackTrace());
			}
		}
	}

	/**
	 * store the (key, value) pairs as metrics in the database
	 *
	 * @param metrics keys and values of the metrics
	 */
	public void storeMetrics(final Map<String, String> metrics, final boolean classMetric) {
		final Document filter = new Document();
		this.projectKeys.stream().forEach(key -> filter.append(key, metrics.get(key)));
		if (classMetric) {
			filter.append(CLASS, metrics.get(CLASS));
		}
		final Document doc = new Document();
		metrics.entrySet().stream().forEach(entry -> doc.append(entry.getKey(), entry.getValue()));
		final Document updateDoc = new Document();
		updateDoc.put("$set", doc);
		final UpdateOptions options = new UpdateOptions();
		options.upsert(true);
		this.dbCollection.updateOne(filter, updateDoc, options);
	}

	public void storeMetrics(final List<Map<String, String>> metricsList, final boolean classMetrics) {
		for (final Map<String, String> metrics : metricsList) {
			storeMetrics(metrics, classMetrics);
		}
	}

	/**
	 * get all metrics from projects that fit the given
	 *
	 * @param filterMap filter values (e.g. version and product), can be more
	 *                  complicated like lloc > 100,000
	 * @return list of all metrics from projects that matched the filter
	 */
	public List<Map<String, String>> getMetrics(final Map<String, Object> filterMap) {
		final List<Map<String, String>> results = new LinkedList<>();
		final Document filter = new Document(filterMap);
		for (final Document d : this.dbCollection.find(filter)) {
			final Map<String, String> next = new HashMap<>();
			d.entrySet().stream().filter(entry -> !(entry.getKey().equals("_id")))
			.forEach(entry -> next.put(entry.getKey(), (String) entry.getValue()));
			results.add(next);
		}
		return results;
	}

	public List<Document> sampleDocs(final String[] types, final int size) {
		final List<Document> result = new ArrayList<>();
		final List<Bson> filterList = new ArrayList<>();
		for (final String s : types) {
			filterList.add(eq("type", s));
		}
		final AggregateIterable<Document> docs = this.dbCollection.aggregate(Arrays.asList(match(or(filterList)), sample(size)));
		for (final Document doc : docs) {
			result.add(doc);
		}
		return result;
	}

	public long delete(final List<Document> docs) {
		final List<ObjectId> ids = new ArrayList<>();
		for (final Document doc : docs) {
			ids.add((ObjectId) doc.get("_id"));
		}
		final DeleteResult result = this.dbCollection.deleteMany(Filters.in("_id", ids));
		return result.getDeletedCount();
	}

	public void storeData(final Map<String, String> data) {
		final Document doc = new Document();
		data.entrySet().stream().forEach(entry -> doc.append(entry.getKey(), entry.getValue()));
		this.dbCollection.insertOne(doc);
	}

	public void storeMany(final List<Map<String, Object>> dataList) {
		final List<Document> docs = new LinkedList<>();
		for (final Map<String, Object> data : dataList) {
			final Document doc = new Document();
			data.entrySet().stream().forEach(entry -> doc.append(entry.getKey(), entry.getValue()));
			docs.add(doc);
		}
		this.dbCollection.insertMany(docs);
	}

	public void addDocuments(final List<Document> docs) {
		this.dbCollection.insertMany(docs);
	}

	public void cleanCollection() {
		this.dbCollection.drop();
		this.client.getDatabase(this.databaseName).createCollection(this.collectionName);
		this.dbCollection = this.client.getDatabase(this.databaseName).getCollection(this.collectionName);
	}

	public List<Document> getDocuments(final Map<String, Object> filterMap) {
		final Document filter = new Document(filterMap);
		final List<Document> result = new ArrayList<>();
		for (final Document doc : this.dbCollection.find(filter)) {
			result.add(doc);
		}
		return result;
	}

	/**
	 * close the db connection
	 */
	@Override
	public void close() {
		this.client.close();
	}

	/**
	 * shuts down the database, you should close all clients before calling this
	 */
	public static void shutdownDatabase() {
		final Document shutdownDoc = new Document();
		shutdownDoc.append("shutdown", 1);
		try (MongoClient shutdownClient = new MongoClient(new MongoClientURI(SERVER_URI))) {
			shutdownClient.getDatabase("admin").runCommand(shutdownDoc);
		} catch (final MongoSocketReadException e) { // the shutdown always throws an exception but works
			LOGGER.log(Level.INFO, "Shutdown mongodb server");
		}

	}

	/**
	 * if the config file is not set as parameter, mondodb will choose default
	 * values and ignore .cfg files in the folder
	 *
	 * @return absolute location of the config file, if present
	 */
	private static String findConfig() {
		final File mongodParent = getMongoDBFolder();
		final Optional<File> configOpt = Arrays.stream(mongodParent.listFiles())
				.filter(f -> FilenameUtils.getExtension(f.getName()).equals("cfg")).findFirst();
		if (configOpt.isPresent()) {
			return configOpt.get().getName();
		} else {
			return null;
		}
	}

	/**
	 *
	 * @return absolute path of mongodb folder
	 */
	private static File getMongoDBFolder() {
		return new File(System.getenv(MONGOD)).getParentFile();
	}

	/**
	 * checks if the port is available
	 *
	 * @param port port to check
	 * @return true if port is not in use
	 */
	private static boolean available(final int port) {
		try (Socket ignored = new Socket("localhost", port)) {
			return false;
		} catch (final IOException ignored) {
			return true;
		}
	}

	// THIS IS REALLY INEFFICIENT, MAYBE OPTIMIZE
	public void storeClassMetrics(final String productName, final String vendorName, final String version,
			final Map<String, Map<String, String>> classResults) {
		for (final Entry<String, Map<String, String>> entry : classResults.entrySet()) {
			final Map<String, String> fullDataMap = new HashMap<>(entry.getValue());
			fullDataMap.put("product", productName);
			fullDataMap.put("vendor", vendorName);
			fullDataMap.put("version", version);
			fullDataMap.put(CLASS, entry.getKey());
			storeMetrics(fullDataMap, true);
		}

	}
}
