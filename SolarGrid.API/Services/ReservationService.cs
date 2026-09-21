using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class ReservationService
{
    private readonly IMongoCollection<EnergyReservation> _reservations;

    public ReservationService(MongoDbContext dbContext)
    {
        _reservations = dbContext.Database.GetCollection<EnergyReservation>("Reservations");
    }
}
