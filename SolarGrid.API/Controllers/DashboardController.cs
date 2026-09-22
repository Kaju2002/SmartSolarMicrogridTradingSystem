using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class DashboardController : ControllerBase
{
    private readonly IVerificationService _verificationService;

    public DashboardController(IVerificationService verificationService)
    {
        _verificationService = verificationService;
    }

    [HttpGet("summary/{prosumerNic}")]
    public async Task<IActionResult> GetSummary(string prosumerNic)
    {
        var result = await _verificationService.GetDashboardSummaryAsync(prosumerNic);
        return Ok(result);
    }

    [HttpGet("history/{prosumerNic}")]
    public async Task<IActionResult> GetHistory(string prosumerNic)
    {
        var result = await _verificationService.GetBookingHistoryAsync(prosumerNic);
        return Ok(result);
    }

    [HttpGet("search/{prosumerNic}")]
    public async Task<IActionResult> Search(string prosumerNic, [FromQuery] string? query)
    {
        var result = await _verificationService.SearchBookingsAsync(prosumerNic, query);
        return Ok(result);
    }
}
