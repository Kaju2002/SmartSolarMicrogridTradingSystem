/*
 * File: VerificationController.cs
 * Description: QR scan endpoint for operators
 * Author: Aaron (Verification and Dashboard)
 * Date: 22/09/2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/[controller]")]
[Authorize(Roles = "GridOperator,Backoffice")]
public class VerificationController : ControllerBase
{
    private readonly IVerificationService _verificationService;

    // Inject verification service
    public VerificationController(IVerificationService verificationService)
    {
        _verificationService = verificationService;
    }

    // POST scan QR and finalize transfer
    [HttpPost("scan-qr")]
    public async Task<IActionResult> ScanQr([FromBody] VerifyQrDto request)
    {
        var result = await _verificationService.VerifyAndFinalizeAsync(request);
        if (result.Success) return Ok(result);
        return BadRequest(result);
    }
}
