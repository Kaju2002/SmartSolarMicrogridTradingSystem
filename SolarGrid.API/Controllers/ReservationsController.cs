/*
 * File: ReservationsController.cs
 * Description: Reservation API endpoints
 * Author: Kajanthan (Energy Reservation / Booking)
 * Date: 21/09/2026
 */
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

    // POST create reservation — Prosumer
    [Authorize(Roles = "Prosumer")]
    [HttpPost]
    public async Task<IActionResult> Create([FromBody] CreateReservationDto request)
    {
        var result = await _reservationService.CreateReservationAsync(request);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    // PUT update reservation datetime — Prosumer
    [Authorize(Roles = "Prosumer")]
    [HttpPut("{id}")]
    public async Task<IActionResult> Update(string id, [FromBody] UpdateReservationDto request)
    {
        var result = await _reservationService.UpdateReservationAsync(id, request);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    // DELETE cancel reservation — Prosumer or Backoffice
    [Authorize(Roles = "Prosumer,Backoffice")]
    [HttpDelete("{id}")]
    public async Task<IActionResult> Cancel(string id)
    {
        var result = await _reservationService.CancelReservationAsync(id);

        if (result.Success)
            return Ok(result);

        return BadRequest(result);
    }

    // GET all reservations — Backoffice and Grid Operator
    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpGet]
    public async Task<IActionResult> GetAll()
    {
        var list = await _reservationService.GetAllReservationsAsync();
        return Ok(list);
    }

    // GET reservations by prosumer NIC — Prosumer (own) or staff
    [Authorize(Roles = "Prosumer,Backoffice,GridOperator")]
    [HttpGet("prosumer/{nic}")]
    public async Task<IActionResult> GetByProsumer(string nic)
    {
        var list = await _reservationService.GetReservationsByProsumerAsync(nic);
        return Ok(list);
    }

    // PUT approve reservation — Backoffice or Grid Operator; actor from JWT
    [Authorize(Roles = "Backoffice,GridOperator")]
    [HttpPut("{id}/approve")]
    public async Task<IActionResult> Approve(string id)
    {
        var approvedByUserId = User.GetUserId();
        if (string.IsNullOrEmpty(approvedByUserId))
            return Unauthorized();

        var result = await _reservationService.ApproveReservationAsync(id, approvedByUserId);

        if (result.Success)
            return Ok(result);

        return NotFound(result);
    }
}
