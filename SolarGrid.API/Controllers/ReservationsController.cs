/*
 * File: ReservationsController.cs
 * Description: Reservation API endpoints
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 21/09/2026
 */
using System.Globalization;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Helpers;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize]
public class ReservationsController : ControllerBase
{
    private readonly IReservationService _reservationService;

    // Inject reservation service
    public ReservationsController(IReservationService reservationService)
    {
        _reservationService = reservationService;
    }

    // NIC of the signed-in Prosumer from the JWT user id
    private async Task<string?> CurrentProsumerNicAsync()
    {
        var userId = User.GetUserId();
        if (string.IsNullOrEmpty(userId))
            return null;

        return await _reservationService.GetProsumerNicAsync(userId);
    }

    // POST create reservation — Prosumer; NIC comes from the token, not the body
    [Authorize(Roles = "Prosumer")]
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateReservationDto request)
    {
        var nic = await CurrentProsumerNicAsync();
        if (nic is null)
            return Forbid();

        request.ProsumerNic = nic;

        var result = await _reservationService.CreateReservationAsync(request);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    // PUT update reservation datetime — Prosumer (own bookings only)
    [Authorize(Roles = "Prosumer")]
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationDto request)
    {
        var nic = await CurrentProsumerNicAsync();
        if (nic is null)
            return Forbid();

        var result = await _reservationService.UpdateReservationAsync(id, request, nic);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    // DELETE cancel reservation — Prosumer (own) or Backoffice (any)
    [Authorize(Roles = "Prosumer,Backoffice")]
    [HttpDelete("{id}")]
    public async Task<IActionResult> Cancel(string id)
    {
        string? ownerNic = null;
        if (User.IsInRole("Prosumer"))
        {
            ownerNic = await CurrentProsumerNicAsync();
            if (ownerNic is null)
                return Forbid();
        }

        var result = await _reservationService.CancelReservationAsync(id, ownerNic);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    // GET hourly slots for a station on a date (yyyy-MM-dd, Sri Lanka time)
    [Authorize(Roles = "Prosumer,Backoffice,GridOperator")]
    [HttpGet("availability")]
    public async Task<IActionResult> GetAvailability([FromQuery] string? stationId, [FromQuery] string? date)
    {
        if (string.IsNullOrWhiteSpace(stationId) ||
            !DateOnly.TryParseExact(date, "yyyy-MM-dd", CultureInfo.InvariantCulture, DateTimeStyles.None, out var day))
        {
            return BadRequest(new ReservationResponseDto
            {
                Success = false,
                Message = "stationId and date (yyyy-MM-dd) are required"
            });
        }

        var availability = await _reservationService.GetAvailabilityAsync(stationId, day);
        if (availability is null)
        {
            return NotFound(new ReservationResponseDto
            {
                Success = false,
                Message = "Station not found or not taking bookings"
            });
        }

        return Ok(availability);
    }

    // GET all reservations — Backoffice and Grid Operator
    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpGet]
    public async Task<IActionResult> GetAll()
    {
        var list = await _reservationService.GetAllReservationsAsync();
        return Ok(list);
    }

    // GET bookings at the signed-in Grid Operator's stations
    [Authorize(Roles = "GridOperator")]
    [HttpGet("operator")]
    public async Task<IActionResult> GetForOperator()
    {
        var operatorId = User.GetUserId();
        if (string.IsNullOrEmpty(operatorId))
            return Unauthorized();

        var list = await _reservationService.GetOperatorReservationsAsync(operatorId);
        return Ok(list);
    }

    // GET reservations by prosumer NIC — Prosumer (own) or staff
    [Authorize(Roles = "Prosumer,Backoffice,GridOperator")]
    [HttpGet("prosumer/{nic}")]
    public async Task<IActionResult> GetByProsumer(string nic)
    {
        if (User.IsInRole("Prosumer"))
        {
            var ownNic = await CurrentProsumerNicAsync();
            if (ownNic is null ||
                !string.Equals(ownNic, nic.Trim(), StringComparison.OrdinalIgnoreCase))
                return Forbid();
        }

        var list = await _reservationService.GetReservationsByProsumerAsync(nic);
        return Ok(list);
    }

    // PUT approve reservation — Backoffice (any) or Grid Operator (own stations); actor from JWT
    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpPut("{id}/approve")]
    public async Task<IActionResult> Approve(string id)
    {
        var approvedByUserId = User.GetUserId();
        if (string.IsNullOrEmpty(approvedByUserId))
            return Unauthorized();

        var operatorId = User.IsBackoffice() ? null : approvedByUserId;
        var result = await _reservationService.ApproveReservationAsync(id, approvedByUserId, operatorId);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }
}

