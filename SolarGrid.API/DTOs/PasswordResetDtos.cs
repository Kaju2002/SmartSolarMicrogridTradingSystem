/*
 * File: PasswordResetDtos.cs
 * Description: Request and response bodies for the three forgot-password steps
 * Author: Vithusha (Identity and Access)
 * Date: 04/10/2026
 */
namespace SolarGrid.API.DTOs;

public class ForgotPasswordDto
{
    public string Email { get; set; } = string.Empty;
}

public class VerifyResetCodeDto
{
    public string Email { get; set; } = string.Empty;

    public string Code { get; set; } = string.Empty;
}

public class ResetPasswordDto
{
    public string Email { get; set; } = string.Empty;

    public string ResetToken { get; set; } = string.Empty;
    // From verify-reset-code

    public string NewPassword { get; set; } = string.Empty;
}

public class PasswordResetResponseDto
{
    public bool Success { get; set; }

    public string Message { get; set; } = string.Empty;

    public string? ResetToken { get; set; }
    // Only set by verify-reset-code
}
