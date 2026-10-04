/*
 * File: PasswordResetController.cs
 * Description: Forgot-password endpoints under /api/Auth (no login needed)
 * Author: Vithusha (Identity and Access)
 * Date: 04/10/2026
 */
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using SolarGrid.API.DTOs;
using SolarGrid.API.Services;

namespace SolarGrid.API.Controllers;

[ApiController]
[Route("api/Auth")]
[AllowAnonymous]
public class PasswordResetController : ControllerBase
{
    private readonly IPasswordResetService _passwordResetService;

    // Inject password reset service
    public PasswordResetController(IPasswordResetService passwordResetService)
    {
        _passwordResetService = passwordResetService;
    }

    // POST e-mail a 4-digit code
    [HttpPost("forgot-password")]
    public async Task<IActionResult> ForgotPassword([FromBody] ForgotPasswordDto request)
    {
        var result = await _passwordResetService.SendCodeAsync(request);
        return StatusCode(result.StatusCode, result.Body);
    }

    // POST check the code, returns a reset token
    [HttpPost("verify-reset-code")]
    public async Task<IActionResult> VerifyResetCode([FromBody] VerifyResetCodeDto request)
    {
        var result = await _passwordResetService.VerifyCodeAsync(request);
        return StatusCode(result.StatusCode, result.Body);
    }

    // POST set the new password with the reset token
    [HttpPost("reset-password")]
    public async Task<IActionResult> ResetPassword([FromBody] ResetPasswordDto request)
    {
        var result = await _passwordResetService.ResetPasswordAsync(request);
        return StatusCode(result.StatusCode, result.Body);
    }
}
