/*
 * File: IPasswordResetService.cs
 * Description: Forgot-password contract (send code, verify code, set new password)
 * Author: Vithusha (Identity and Access)
 * Date: 04/10/2026
 */
using SolarGrid.API.DTOs;

namespace SolarGrid.API.Services;

// HTTP status for the controller plus the body to send back
public record PasswordResetResult(int StatusCode, PasswordResetResponseDto Body);

public interface IPasswordResetService
{
    // E-mail a 4-digit code; same answer whether or not the e-mail has an account
    Task<PasswordResetResult> SendCodeAsync(ForgotPasswordDto request);
    // Check the code and hand out a short-lived reset token
    Task<PasswordResetResult> VerifyCodeAsync(VerifyResetCodeDto request);
    // Set the new password with the reset token
    Task<PasswordResetResult> ResetPasswordAsync(ResetPasswordDto request);
}
