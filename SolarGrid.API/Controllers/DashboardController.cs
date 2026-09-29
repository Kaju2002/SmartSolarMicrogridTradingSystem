/*
 * File: DashboardController.cs
 * Description: Prosumer dashboard summary, history and search
 * Author: Aaron (Verification and Dashboard)
 * Date: 22/09/2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.Helpers;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Prosumer,Backoffice")]
public class DashboardController : ControllerBase
{
    private readonly IVerificationService _verificationService;
    private readonly IReservationService _reservationService;

    // Inject verification service, plus reservation service to look up the caller's NIC
    public DashboardController(IVerificationService verificationService, IReservationService reservationService)
    {
        _verificationService = verificationService;
        _reservationService = reservationService;
    }

    // Prosumers may only read their own NIC; Backoffice can read any
    private async Task<bool> CanReadNicAsync(string prosumerNic)
    {
        if (!User.IsInRole("Prosumer"))
            return true;

        var userId = User.GetUserId();
        if (string.IsNullOrEmpty(userId))
            return false;

        var ownNic = await _reservationService.GetProsumerNicAsync(userId);
        return ownNic is not null
            && string.Equals(ownNic, prosumerNic.Trim(), StringComparison.OrdinalIgnoreCase);
    }

    // GET booking counts for NIC
    [HttpGet("summary/{prosumerNic}")]
    public async Task<IActionResult> GetSummary(string prosumerNic)
    {
        if (!await CanReadNicAsync(prosumerNic))
            return Forbid();

        var result = await _verificationService.GetDashboardSummaryAsync(prosumerNic);
        return Ok(result);
    }

    // GET completed booking history
    [HttpGet("history/{prosumerNic}")]
    public async Task<IActionResult> GetHistory(string prosumerNic)
    {
        if (!await CanReadNicAsync(prosumerNic))
            return Forbid();

        var result = await _verificationService.GetBookingHistoryAsync(prosumerNic);
        return Ok(result);
    }

    // GET search bookings by status
    [HttpGet("search/{prosumerNic}")]
    public async Task<IActionResult> Search(string prosumerNic, [FromQuery] string? query)
    {
        if (!await CanReadNicAsync(prosumerNic))
            return Forbid();

        var result = await _verificationService.SearchBookingsAsync(prosumerNic, query);
        return Ok(result);
    }
}
