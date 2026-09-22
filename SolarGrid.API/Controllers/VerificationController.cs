using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
public class VerificationController : ControllerBase
{
    private readonly IVerificationService _verificationService;

    public VerificationController(IVerificationService verificationService)
    {
        _verificationService = verificationService;
    }

    [HttpPost("scan-qr")]
    public async Task<IActionResult> ScanQr([FromBody] VerifyQrDto request)
    {
        var result = await _verificationService.VerifyAndFinalizeAsync(request);
        if (result.Success) return Ok(result);
        return BadRequest(result);
    }
}
