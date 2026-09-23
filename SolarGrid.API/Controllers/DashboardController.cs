/*
 * File: DashboardController.cs
 * Description: Prosumer dashboard summary, history and search
 * Author: Aaron (Verification and Dashboard)
 * Date: 22/09/2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "Prosumer,Backoffice")]
public class DashboardController : ControllerBase
{
    private readonly IVerificationService _verificationService;

    // Inject verification service
    public DashboardController(IVerificationService verificationService)
    {
        _verificationService = verificationService;
    }

    // GET booking counts for NIC
    [HttpGet("summary/{prosumerNic}")]
    public async Task<IActionResult> GetSummary(string prosumerNic)
    {
        var result = await _verificationService.GetDashboardSummaryAsync(prosumerNic);
        return Ok(result);
    }

    // GET completed booking history
    [HttpGet("history/{prosumerNic}")]
    public async Task<IActionResult> GetHistory(string prosumerNic)
    {
        var result = await _verificationService.GetBookingHistoryAsync(prosumerNic);
        return Ok(result);
    }

    // GET search bookings by status
    [HttpGet("search/{prosumerNic}")]
    public async Task<IActionResult> Search(string prosumerNic, [FromQuery] string? query)
    {
        var result = await _verificationService.SearchBookingsAsync(prosumerNic, query);
        return Ok(result);
    }
}
