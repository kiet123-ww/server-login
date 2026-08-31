package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;

public class MongoDBConnection {
    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static void connect() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String uri = dotenv.get("MONGO_URI", "mongodb://localhost:27017");
        String dbName = dotenv.get("DB_NAME", "nro_db");
        
        mongoClient = MongoClients.create(uri);
        database = mongoClient.getDatabase(dbName);
        System.out.println("Connected to MongoDB: " + dbName);
    }

    public static MongoDatabase getDatabase() {
        if (database == null) {
            connect();
        }
        return database;
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
