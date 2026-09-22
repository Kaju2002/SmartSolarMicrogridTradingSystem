/*
 * File: MongoDbContext.cs
 * Description: Shared MongoDB database connection
 * Author: Team
 * Date: 20/09/2026
 */
using MongoDB.Driver;

namespace SolarGrid.API.Data;

public class MongoDbContext
{
    private readonly IMongoDatabase _database;

    // Read connection string and open database
    public MongoDbContext(IConfiguration configuration)
    {
        var connectionString = configuration["MongoDbSettings:ConnectionString"];
        var databaseName = configuration["MongoDbSettings:DatabaseName"];

        var client = new MongoClient(connectionString);
        _database = client.GetDatabase(databaseName);
    }

    public IMongoDatabase Database => _database;
}
