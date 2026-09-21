using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class StationService
{
    private readonly IMongoCollection<SolarStationInfo> _stations;
    private readonly IMongoCollection<EnergyReservation> _reservations;

    public StationService(MongoDbContext dbContext)
    {
        _stations = dbContext.Database.GetCollection<SolarStationInfo>("Stations");
        _reservations = dbContext.Database.GetCollection<EnergyReservation>("Reservations");
    }
}
