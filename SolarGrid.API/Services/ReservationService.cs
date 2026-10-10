/*
 * File: ReservationService.cs
 * Description: Booking create, update, cancel and approve
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 21/09/2026
 */
using MongoDB.Bson;
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Helpers;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class ReservationService : IReservationService
{
    private static readonly string[] LiveStatuses = { "Pending", "Approved" };

    // Smallest energy request per booking
    public const double MinBookingKWh = 1;

    // Room for floating point drift when adding kWh
    private const double KWhTolerance = 0.000001;

    // Index is created once per app run, not per request
    private static int _usageIndexReady;

    private readonly IMongoCollection<EnergyReservation> _reservations;
    private readonly IMongoCollection<EnergyBookingSlot> _bookingSlots;
    private readonly IMongoCollection<SlotUsage> _slotUsage;
    private readonly IMongoCollection<User> _users;
    private readonly IMongoCollection<SolarStationInfo> _stations;

    // Setup Reservations, EnergyBookingSlots, SlotUsage, Users and Stations collections
    public ReservationService(MongoDbContext dbContext)
    {
        _reservations = dbContext.Database.GetCollection<EnergyReservation>("Reservations");
        _bookingSlots = dbContext.Database.GetCollection<EnergyBookingSlot>("EnergyBookingSlots");
        _slotUsage = dbContext.Database.GetCollection<SlotUsage>("SlotUsage");
        _users = dbContext.Database.GetCollection<User>("Users");
        _stations = dbContext.Database.GetCollection<SolarStationInfo>("Stations");

        EnsureUsageIndex();
    }

    // One counter per station hour; the unique index stops two requests making two counters
    private void EnsureUsageIndex()
    {
        if (Interlocked.Exchange(ref _usageIndexReady, 1) == 1)
            return;

        try
        {
            var keys = Builders<SlotUsage>.IndexKeys
                .Ascending(u => u.StationId)
                .Ascending(u => u.SlotTime);
            _slotUsage.Indexes.CreateOne(new CreateIndexModel<SlotUsage>(keys, new CreateIndexOptions { Unique = true }));
        }
        catch
        {
            Interlocked.Exchange(ref _usageIndexReady, 0);
            throw;
        }
    }

    // Failed action response
    private static ReservationResponseDto Fail(string message) =>
        new() { Success = false, Message = message };

    // Active station by id, or null for bad / unknown / deactivated ids
    private async Task<SolarStationInfo?> FindActiveStationAsync(string stationId)
    {
        var id = stationId.Trim();
        if (!ObjectId.TryParse(id, out _))
            return null;

        return await _stations
            .Find(s => s.Id == id && s.Status == "Active")
            .FirstOrDefaultAsync();
    }

    // Bookings allowed at the same hour, one per battery slot
    private static int SpotsPerSlot(SolarStationInfo station) => Math.Max(station.BatterySlots, 1);

    // Station capacity is shared by all bookings in the same hour
    private static double KWhLeft(SolarStationInfo station, double bookedKWh) =>
        Math.Max(station.CapacityKWh - bookedKWh, 0);

    // LKR estimate from the station rate at booking time
    private static double EstimateCost(SolarStationInfo station, double kWh) =>
        Math.Round(kWh * station.RatePerKwh, 2);

    // Booked spots and kWh at one station and time, ignoring the caller's own slot
    private async Task<(int Spots, double KWh)> BookedUsageAsync(string stationId, DateTime slotUtc, string? excludeSlotId)
    {
        var builder = Builders<EnergyBookingSlot>.Filter;
        var filter = builder.Eq(s => s.StationId, stationId)
                     & builder.Eq(s => s.SlotDateTime, slotUtc)
                     & builder.Eq(s => s.SlotStatus, "Booked");

        if (excludeSlotId is not null)
            filter &= builder.Ne(s => s.Id, excludeSlotId);

        var booked = await _bookingSlots.Find(filter).ToListAsync();
        return (booked.Count, booked.Sum(s => s.CapacityKWh));
    }

    private static FilterDefinition<SlotUsage> UsageKey(string stationId, DateTime slotUtc) =>
        Builders<SlotUsage>.Filter.Eq(u => u.StationId, stationId)
        & Builders<SlotUsage>.Filter.Eq(u => u.SlotTime, slotUtc);

    // First booking at an hour starts its counter from the slots already booked there
    private async Task EnsureUsageAsync(string stationId, DateTime slotUtc)
    {
        if (await _slotUsage.Find(UsageKey(stationId, slotUtc)).AnyAsync())
            return;

        var (spots, kWh) = await BookedUsageAsync(stationId, slotUtc, null);
        try
        {
            await _slotUsage.InsertOneAsync(new SlotUsage
            {
                StationId = stationId,
                SlotTime = slotUtc,
                BookedCount = spots,
                BookedKWh = kWh
            });
        }
        catch (MongoWriteException e) when (e.WriteError.Category == ServerErrorCategory.DuplicateKey)
        {
            // Another request made the counter first; use theirs
        }
    }

    // Takes a spot and kWh in one atomic update; false if the hour is already full
    private async Task<bool> TryReserveAsync(SolarStationInfo station, DateTime slotUtc, double kWh)
    {
        await EnsureUsageAsync(station.Id!, slotUtc);

        var f = Builders<SlotUsage>.Filter;
        var hasRoom = UsageKey(station.Id!, slotUtc)
                      & f.Lt(u => u.BookedCount, SpotsPerSlot(station))
                      & f.Lte(u => u.BookedKWh, station.CapacityKWh - kWh + KWhTolerance);

        var result = await _slotUsage.UpdateOneAsync(hasRoom,
            Builders<SlotUsage>.Update.Inc(u => u.BookedCount, 1).Inc(u => u.BookedKWh, kWh));

        return result.ModifiedCount == 1;
    }

    // Gives a spot and its kWh back to the hour
    private async Task ReleaseAsync(string stationId, DateTime slotUtc, double kWh)
    {
        var filter = UsageKey(stationId, slotUtc) & Builders<SlotUsage>.Filter.Gt(u => u.BookedCount, 0);
        await _slotUsage.UpdateOneAsync(filter,
            Builders<SlotUsage>.Update.Inc(u => u.BookedCount, -1).Inc(u => u.BookedKWh, -kWh));
    }

    // Station, 7-day window, on-the-hour start, opening hours, free spots and kWh; error or the station
    private async Task<(string? Error, SolarStationInfo? Station)> ValidateSlotAsync(
        string stationId, DateTime slotUtc, double requestedKWh, string? excludeSlotId)
    {
        var station = await FindActiveStationAsync(stationId);
        if (station is null)
            return ("Station not found or not taking bookings", null);

        if (!BookingTime.IsWithinWindow(slotUtc))
            return ($"Reservation must be within {BookingTime.BookingWindowDays} days", null);

        var local = BookingTime.ToLocal(slotUtc);
        if (local.Minute != 0 || local.Second != 0 || local.Millisecond != 0)
            return ("Slots start on the hour", null);

        var open = BookingTime.ParseHour(station.OpenTime);
        var close = BookingTime.ParseHour(station.CloseTime);
        if (open is null || close is null)
            return ("Station opening hours are not set", null);

        var start = local.TimeOfDay;
        if (start < open.Value.ToTimeSpan() || start + BookingTime.SlotLength > close.Value.ToTimeSpan())
            return ($"Station is open {station.OpenTime} to {station.CloseTime}", null);

        var (spots, bookedKWh) = await BookedUsageAsync(station.Id!, slotUtc, excludeSlotId);
        if (spots >= SpotsPerSlot(station))
            return ("This time slot is full", null);

        var kWhLeft = KWhLeft(station, bookedKWh);
        if (requestedKWh > kWhLeft)
            return ($"Only {kWhLeft:0.#} kWh left at this time", null);

        return (null, station);
    }

    // True if the prosumer already holds a live booking at this time
    private async Task<bool> HasClashAsync(string nic, DateTime slotUtc, string? excludeReservationId)
    {
        var builder = Builders<EnergyReservation>.Filter;
        var filter = builder.Eq(r => r.ProsumerNic, nic)
                     & builder.Eq(r => r.ReservationDateTime, slotUtc)
                     & builder.In(r => r.Status, LiveStatuses);

        if (excludeReservationId is not null)
            filter &= builder.Ne(r => r.Id, excludeReservationId);

        return await _reservations.Find(filter).AnyAsync();
    }

    // NIC of a Prosumer account, or null for staff / unknown users
    public async Task<string?> GetProsumerNicAsync(string userId)
    {
        var user = await _users.Find(u => u.Id == userId).FirstOrDefaultAsync();
        if (user is null || user.UserType != "Prosumer")
            return null;

        return string.IsNullOrWhiteSpace(user.Nic) ? null : user.Nic.Trim();
    }

    // Only live bookings can be changed or cancelled
    private static bool IsChangeable(EnergyReservation reservation) =>
        reservation.Status == "Pending" || reservation.Status == "Approved";

    // ownerNic null = staff caller, no ownership check
    private static bool IsOwnedBy(EnergyReservation reservation, string? ownerNic) =>
        ownerNic is null ||
        string.Equals(reservation.ProsumerNic.Trim(), ownerNic.Trim(), StringComparison.OrdinalIgnoreCase);

    // Create booking after the kWh, slot and prosumer clash checks; cost is worked out here
    public async Task<ReservationResponseDto> CreateReservationAsync(CreateReservationDto request)
    {
        var stationId = request.StationId.Trim();
        var slotUtc = BookingTime.ToUtc(request.ReservationDateTime);
        var requestedKWh = request.RequestedKWh;

        if (double.IsNaN(requestedKWh) || requestedKWh < MinBookingKWh)
            return Fail($"Request at least {MinBookingKWh:0.#} kWh");

        var (slotError, station) = await ValidateSlotAsync(stationId, slotUtc, requestedKWh, null);
        if (slotError is not null || station is null)
            return Fail(slotError ?? "Station not found or not taking bookings");

        if (await HasClashAsync(request.ProsumerNic, slotUtc, null))
            return Fail("You already have a booking at this time");

        // Checks above can race; this atomic step is what actually holds the spot
        if (!await TryReserveAsync(station, slotUtc, requestedKWh))
            return Fail("This time slot is full");

        // Booked slot holds this booking's share of the hour's capacity
        var newSlot = new EnergyBookingSlot
        {
            StationId = stationId,
            SlotDateTime = slotUtc,
            SlotStatus = "Booked",
            CapacityKWh = requestedKWh,
            CreatedAt = DateTime.UtcNow
        };

        var newReservation = new EnergyReservation
        {
            ProsumerNic = request.ProsumerNic,
            StationId = stationId,
            ReservationDateTime = slotUtc,
            RequestedKWh = requestedKWh,
            EstimatedCost = EstimateCost(station, requestedKWh),
            Status = "Pending",
            CreatedAt = DateTime.UtcNow
        };

        try
        {
            await _bookingSlots.InsertOneAsync(newSlot);
            newReservation.BookingSlotId = newSlot.Id;
            await _reservations.InsertOneAsync(newReservation);
        }
        catch
        {
            // Booking wasn't saved, so give the spot back
            if (newSlot.Id is not null)
                await _bookingSlots.DeleteOneAsync(s => s.Id == newSlot.Id);
            await ReleaseAsync(stationId, slotUtc, requestedKWh);
            throw;
        }

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation created",
            ReservationId = newReservation.Id,
            Status = "Pending",
            ReservationDateTime = newReservation.ReservationDateTime,
            RequestedKWh = newReservation.RequestedKWh,
            EstimatedCost = newReservation.EstimatedCost
        };
    }

    // Change slot time if more than 12 hours left
    public async Task<ReservationResponseDto> UpdateReservationAsync(
        string reservationId,
        UpdateReservationDto request,
        string? ownerNic)
    {
        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.Id, reservationId);
        var existingReservation = await _reservations.Find(filter).FirstOrDefaultAsync();

        // Someone else's booking looks the same as a missing one
        if (existingReservation is null || !IsOwnedBy(existingReservation, ownerNic))
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Reservation not found"
            };
        }

        if (!IsChangeable(existingReservation))
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = $"{existingReservation.Status} bookings cannot be changed"
            };
        }

        var hoursUntilSlot = (existingReservation.ReservationDateTime - DateTime.UtcNow).TotalHours;
        if (hoursUntilSlot < 12)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Cannot update within 12 hours of reservation"
            };
        }

        // New time follows the same slot rules with the same kWh, ignoring this booking's own spot
        var newSlotUtc = BookingTime.ToUtc(request.NewReservationDateTime);
        var (slotError, station) = await ValidateSlotAsync(
            existingReservation.StationId, newSlotUtc, existingReservation.RequestedKWh,
            existingReservation.BookingSlotId);
        if (slotError is not null || station is null)
            return Fail(slotError ?? "Station not found or not taking bookings");

        if (await HasClashAsync(existingReservation.ProsumerNic, newSlotUtc, existingReservation.Id))
            return Fail("You already have a booking at this time");

        var oldSlotUtc = BookingTime.ToUtc(existingReservation.ReservationDateTime);
        var kWh = existingReservation.RequestedKWh;
        var moving = existingReservation.BookingSlotId is not null && newSlotUtc != oldSlotUtc;

        // Hold the new hour before letting go of the old one, so the old spot is never lost
        if (moving && !await TryReserveAsync(station, newSlotUtc, kWh))
            return Fail("This time slot is full");

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.ReservationDateTime, newSlotUtc)
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        try
        {
            // Still-live filter stops a booking cancelled meanwhile from being moved
            var stillLive = filter & Builders<EnergyReservation>.Filter.In(r => r.Status, LiveStatuses);
            var result = await _reservations.UpdateOneAsync(stillLive, update);
            if (result.ModifiedCount == 0)
            {
                if (moving)
                    await ReleaseAsync(existingReservation.StationId, newSlotUtc, kWh);
                return Fail("Reservation can no longer be changed");
            }

            // Keep linked slot datetime in sync
            if (existingReservation.BookingSlotId is not null)
            {
                var linkedSlotFilter = Builders<EnergyBookingSlot>.Filter.Eq(s => s.Id, existingReservation.BookingSlotId);
                var slotUpdate = Builders<EnergyBookingSlot>.Update
                    .Set(s => s.SlotDateTime, newSlotUtc)
                    .Set(s => s.SlotStatus, "Booked");

                await _bookingSlots.UpdateOneAsync(linkedSlotFilter, slotUpdate);
            }
        }
        catch
        {
            if (moving)
                await ReleaseAsync(existingReservation.StationId, newSlotUtc, kWh);
            throw;
        }

        if (moving)
            await ReleaseAsync(existingReservation.StationId, oldSlotUtc, kWh);

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation updated",
            ReservationId = existingReservation.Id,
            Status = existingReservation.Status,
            ReservationDateTime = newSlotUtc,
            RequestedKWh = existingReservation.RequestedKWh,
            EstimatedCost = existingReservation.EstimatedCost
        };
    }

    // Hourly slots inside station hours with spots and kWh left; null if the station is unknown
    public async Task<SlotAvailabilityDto?> GetAvailabilityAsync(string stationId, DateOnly date)
    {
        var station = await FindActiveStationAsync(stationId);
        if (station is null)
            return null;

        var spots = SpotsPerSlot(station);
        var result = new SlotAvailabilityDto
        {
            StationId = station.Id!,
            Date = date.ToString("yyyy-MM-dd"),
            OpenTime = station.OpenTime,
            CloseTime = station.CloseTime,
            SpotsPerSlot = spots,
            CapacityKWh = station.CapacityKWh,
            RatePerKwh = station.RatePerKwh,
            MinKWh = MinBookingKWh
        };

        var open = BookingTime.ParseHour(station.OpenTime);
        var close = BookingTime.ParseHour(station.CloseTime);
        if (open is null || close is null)
            return result;

        var dayStartLocal = date.ToDateTime(TimeOnly.MinValue);
        var dayStartUtc = BookingTime.FromLocal(dayStartLocal);

        var slotBuilder = Builders<EnergyBookingSlot>.Filter;
        var dayFilter = slotBuilder.Eq(s => s.StationId, station.Id)
                        & slotBuilder.Gte(s => s.SlotDateTime, dayStartUtc)
                        & slotBuilder.Lt(s => s.SlotDateTime, dayStartUtc.AddDays(1))
                        & slotBuilder.Eq(s => s.SlotStatus, "Booked");

        var bookedByHour = (await _bookingSlots.Find(dayFilter).ToListAsync())
            .GroupBy(s => BookingTime.ToUtc(s.SlotDateTime))
            .ToDictionary(g => g.Key, g => (Spots: g.Count(), KWh: g.Sum(s => s.CapacityKWh)));

        // First slot is the first full hour at or after opening
        var start = TimeSpan.FromHours(Math.Ceiling(open.Value.ToTimeSpan().TotalHours));
        var closing = close.Value.ToTimeSpan();

        for (; start + BookingTime.SlotLength <= closing; start += BookingTime.SlotLength)
        {
            var slotUtc = BookingTime.FromLocal(dayStartLocal + start);
            var booked = bookedByHour.GetValueOrDefault(slotUtc);
            var spotsLeft = Math.Max(spots - booked.Spots, 0);
            var kWhLeft = KWhLeft(station, booked.KWh);

            result.Slots.Add(new BookingSlotDto
            {
                SlotDateTime = slotUtc,
                StartTime = start.ToString(@"hh\:mm"),
                EndTime = (start + BookingTime.SlotLength).ToString(@"hh\:mm"),
                SpotsLeft = spotsLeft,
                KWhLeft = kWhLeft,
                Status = !BookingTime.IsWithinWindow(slotUtc) ? "Unavailable"
                    : spotsLeft == 0 || kWhLeft < MinBookingKWh ? "Full"
                    : "Available"
            });
        }

        return result;
    }

    // True if the booking's station is assigned to this Grid Operator; operatorId null = no check
    private async Task<bool> IsAtOperatorStationAsync(EnergyReservation reservation, string? operatorId)
    {
        if (operatorId is null)
            return true;

        if (!ObjectId.TryParse(reservation.StationId, out _))
            return false;

        return await _stations
            .Find(s => s.Id == reservation.StationId && s.AssignedOperatorId == operatorId)
            .AnyAsync();
    }

    // Cancel booking and free the linked slot; same 12-hour rule for prosumers and staff
    public async Task<ReservationResponseDto> CancelReservationAsync(
        string reservationId, string? ownerNic, string? operatorId)
    {
        if (!ObjectId.TryParse(reservationId, out _))
            return Fail("Reservation not found");

        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.Id, reservationId);
        var existingReservation = await _reservations.Find(filter).FirstOrDefaultAsync();

        // Someone else's booking, or one at another operator's station, looks the same as a missing one
        if (existingReservation is null || !IsOwnedBy(existingReservation, ownerNic) ||
            !await IsAtOperatorStationAsync(existingReservation, operatorId))
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Reservation not found"
            };
        }

        if (!IsChangeable(existingReservation))
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = $"{existingReservation.Status} bookings cannot be cancelled"
            };
        }

        var hoursUntilSlot = (existingReservation.ReservationDateTime - DateTime.UtcNow).TotalHours;
        if (hoursUntilSlot < 12)
        {
            return new ReservationResponseDto
            {
                Success = false,
                Message = "Cannot cancel within 12 hours of reservation"
            };
        }

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Cancelled")
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        // Still-live filter stops a double cancel from freeing the spot twice
        var stillLive = filter & Builders<EnergyReservation>.Filter.In(r => r.Status, LiveStatuses);
        var result = await _reservations.UpdateOneAsync(stillLive, update);
        if (result.ModifiedCount == 0)
            return Fail("Reservation can no longer be cancelled");

        // Release linked booking slot, and its spot in the hour's counter
        if (existingReservation.BookingSlotId is not null)
        {
            var slotBuilder = Builders<EnergyBookingSlot>.Filter;
            var slotFilter = slotBuilder.Eq(s => s.Id, existingReservation.BookingSlotId)
                             & slotBuilder.Eq(s => s.SlotStatus, "Booked");
            var slotUpdate = Builders<EnergyBookingSlot>.Update.Set(s => s.SlotStatus, "Available");
            var freed = await _bookingSlots.UpdateOneAsync(slotFilter, slotUpdate);

            if (freed.ModifiedCount == 1)
                await ReleaseAsync(existingReservation.StationId,
                    BookingTime.ToUtc(existingReservation.ReservationDateTime), existingReservation.RequestedKWh);
        }

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation cancelled",
            ReservationId = existingReservation.Id,
            Status = "Cancelled",
            ReservationDateTime = existingReservation.ReservationDateTime,
            ProsumerNic = existingReservation.ProsumerNic
        };
    }

    // Get all reservations
    public async Task<List<EnergyReservation>> GetAllReservationsAsync()
    {
        return await _reservations.Find(_ => true).ToListAsync();
    }

    // Get reservations for one prosumer by NIC
    public async Task<List<EnergyReservation>> GetReservationsByProsumerAsync(string nic)
    {
        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.ProsumerNic, nic);
        return await _reservations.Find(filter).ToListAsync();
    }

    // Bookings at the stations assigned to one Grid Operator, soonest slot first
    public async Task<List<OperatorReservationDto>> GetOperatorReservationsAsync(string operatorId)
    {
        var stations = await _stations.Find(s => s.AssignedOperatorId == operatorId).ToListAsync();
        if (stations.Count == 0)
            return new List<OperatorReservationDto>();

        var stationNames = stations.ToDictionary(s => s.Id!, s => s.StationName);
        var reservations = await _reservations
            .Find(Builders<EnergyReservation>.Filter.In(r => r.StationId, stationNames.Keys))
            .SortBy(r => r.ReservationDateTime)
            .ToListAsync();

        return reservations.Select(r => new OperatorReservationDto
        {
            Id = r.Id!,
            ProsumerNic = r.ProsumerNic,
            StationId = r.StationId,
            StationName = stationNames.GetValueOrDefault(r.StationId, string.Empty),
            ReservationDateTime = r.ReservationDateTime,
            RequestedKWh = r.RequestedKWh,
            EstimatedCost = r.EstimatedCost,
            Status = r.Status,
            QrCode = r.QrCode,
            CreatedAt = r.CreatedAt,
            LastModifiedAt = r.LastModifiedAt
        }).ToList();
    }

    // Approve a Pending booking and create its QR code; operatorId null = Backoffice (any station)
    public async Task<ReservationResponseDto> ApproveReservationAsync(
        string reservationId, string approvedByUserId, string? operatorId)
    {
        if (!ObjectId.TryParse(reservationId, out _))
            return Fail("Reservation not found");

        var filter = Builders<EnergyReservation>.Filter.Eq(r => r.Id, reservationId);
        var existingReservation = await _reservations.Find(filter).FirstOrDefaultAsync();
        if (existingReservation is null)
            return Fail("Reservation not found");

        if (existingReservation.Status != "Pending")
            return Fail($"{existingReservation.Status} bookings cannot be approved");

        var station = ObjectId.TryParse(existingReservation.StationId, out _)
            ? await _stations.Find(s => s.Id == existingReservation.StationId).FirstOrDefaultAsync()
            : null;

        if (operatorId is not null && station?.AssignedOperatorId != operatorId)
            return Fail("This booking is not at your station");

        if (station is null || station.Status != "Active")
            return Fail("Station is not taking bookings");

        var slotEndUtc = BookingTime.ToUtc(existingReservation.ReservationDateTime) + BookingTime.SlotLength;
        if (slotEndUtc <= DateTime.UtcNow)
            return Fail("This slot has already passed");

        var qrCode = Guid.NewGuid().ToString();

        var update = Builders<EnergyReservation>.Update
            .Set(r => r.Status, "Approved")
            .Set(r => r.ApprovedBy, approvedByUserId)
            .Set(r => r.QrCode, qrCode)
            .Set(r => r.LastModifiedAt, DateTime.UtcNow);

        // Still-Pending filter stops a second approve from replacing the prosumer's QR
        var stillPending = filter & Builders<EnergyReservation>.Filter.Eq(r => r.Status, "Pending");
        var result = await _reservations.UpdateOneAsync(stillPending, update);
        if (result.ModifiedCount == 0)
            return Fail("Reservation is no longer pending");

        return new ReservationResponseDto
        {
            Success = true,
            Message = "Reservation approved",
            ReservationId = existingReservation.Id,
            Status = "Approved",
            ReservationDateTime = existingReservation.ReservationDateTime,
            RequestedKWh = existingReservation.RequestedKWh,
            EstimatedCost = existingReservation.EstimatedCost,
            QrCode = qrCode,
            ProsumerNic = existingReservation.ProsumerNic,
            StationName = station.StationName
        };
    }
}
