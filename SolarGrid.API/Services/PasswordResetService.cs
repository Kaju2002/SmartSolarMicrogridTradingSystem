/*
 * File: PasswordResetService.cs
 * Description: Forgot password in three steps. A 4-digit code is e-mailed (hashed in the DB,
 *              10 minutes, 5 tries), a correct code returns a one-time reset token, and the
 *              token sets the new password.
 * Author: Vithusha (Identity and Access)
 * Date: 04/10/2026
 */
using System.ComponentModel.DataAnnotations;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using Microsoft.AspNetCore.WebUtilities;
using MongoDB.Bson;
using MongoDB.Driver;
using SolarGrid.API.Data;
using SolarGrid.API.DTOs;
using SolarGrid.API.Models;

namespace SolarGrid.API.Services;

public class PasswordResetService : IPasswordResetService
{
    private const int MinPasswordLength = 6;
    private const int MaxAttempts = 5;
    private static readonly TimeSpan CodeLifetime = TimeSpan.FromMinutes(10);
    private static readonly TimeSpan ResendWait = TimeSpan.FromSeconds(30);
    private static readonly TimeSpan ResetTokenLifetime = TimeSpan.FromMinutes(10);
    private static readonly Regex CodeRegex = new(@"^\d{4}$", RegexOptions.Compiled);

    private const string CodeSentMessage = "If an account uses this e-mail, a code has been sent.";
    private const string BadCodeMessage = "The code is not correct or has expired. Request a new code.";

    private readonly IMongoCollection<User> _users;
    private readonly IMongoCollection<PasswordReset> _resets;
    private readonly IEmailSender _emailSender;
    private readonly IWebHostEnvironment _environment;
    private readonly ILogger<PasswordResetService> _logger;

    // Setup Users and PasswordResets collections and the e-mail sender
    public PasswordResetService(
        MongoDbContext dbContext,
        IEmailSender emailSender,
        IWebHostEnvironment environment,
        ILogger<PasswordResetService> logger)
    {
        _users = dbContext.Database.GetCollection<User>("Users");
        _resets = dbContext.Database.GetCollection<PasswordReset>("PasswordResets");
        _emailSender = emailSender;
        _environment = environment;
        _logger = logger;
    }

    // Step 1: create a code and e-mail it
    public async Task<PasswordResetResult> SendCodeAsync(ForgotPasswordDto request)
    {
        var email = NormaliseEmail(request.Email);
        if (!new EmailAddressAttribute().IsValid(email) || !email.Contains('.'))
            return Fail(StatusCodes.Status400BadRequest, "E-mail is not valid");

        // Checked before the account look-up so the answer never hints whether the e-mail exists
        if (!_emailSender.IsConfigured && !_environment.IsDevelopment())
            return Fail(StatusCodes.Status503ServiceUnavailable, "E-mail service is not set up. Please contact support.");

        var user = await FindUserByEmailAsync(email);
        if (user is null)
            return Ok(CodeSentMessage);

        // A code sent in the last 30 seconds stays valid; matches the app's resend countdown
        var latest = await _resets.Find(r => r.Email == email).SortByDescending(r => r.CreatedAt).FirstOrDefaultAsync();
        if (latest is not null && latest.CreatedAt > DateTime.UtcNow - ResendWait)
            return Ok(CodeSentMessage);

        var code = RandomNumberGenerator.GetInt32(0, 10_000).ToString("D4");
        await _resets.DeleteManyAsync(r => r.Email == email);
        var reset = new PasswordReset
        {
            UserId = user.Id!,
            Email = email,
            CodeHash = BCrypt.Net.BCrypt.HashPassword(code),
            CodeExpiresAt = DateTime.UtcNow + CodeLifetime,
            CreatedAt = DateTime.UtcNow
        };
        await _resets.InsertOneAsync(reset);

        if (!_emailSender.IsConfigured)
        {
            // Development without SMTP only: the code goes to the API console for local testing
            _logger.LogWarning("SMTP not configured. Password reset code for {Email}: {Code}", email, code);
            return Ok(CodeSentMessage);
        }

        try
        {
            await _emailSender.SendAsync(email, "Your SolarGrid password reset code", BuildEmailBody(user.FullName, code));
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Password reset e-mail failed for user {UserId}", user.Id);
            await _resets.DeleteOneAsync(r => r.Id == reset.Id);
            return Fail(StatusCodes.Status503ServiceUnavailable, "Could not send the e-mail. Please try again later.");
        }

        return Ok(CodeSentMessage);
    }

    // Step 2: check the code; a wrong one uses up a try
    public async Task<PasswordResetResult> VerifyCodeAsync(VerifyResetCodeDto request)
    {
        var email = NormaliseEmail(request.Email);
        var code = request.Code?.Trim() ?? string.Empty;
        if (!CodeRegex.IsMatch(code))
            return Fail(StatusCodes.Status400BadRequest, "Enter the 4-digit code");

        var reset = await _resets.Find(r => r.Email == email).SortByDescending(r => r.CreatedAt).FirstOrDefaultAsync();
        var usable = reset is not null
            && reset.CodeHash.Length > 0
            && reset.CodeExpiresAt > DateTime.UtcNow
            && reset.Attempts < MaxAttempts;
        if (!usable)
            return Fail(StatusCodes.Status400BadRequest, BadCodeMessage);

        if (!BCrypt.Net.BCrypt.Verify(code, reset!.CodeHash))
        {
            await _resets.UpdateOneAsync(r => r.Id == reset.Id, Builders<PasswordReset>.Update.Inc(r => r.Attempts, 1));
            var left = MaxAttempts - reset.Attempts - 1;
            return Fail(StatusCodes.Status400BadRequest,
                left > 0 ? $"The code is not correct. {left} tries left." : BadCodeMessage);
        }

        // Code is single use; from here only the token works
        var token = WebEncoders.Base64UrlEncode(RandomNumberGenerator.GetBytes(32));
        var update = Builders<PasswordReset>.Update
            .Set(r => r.CodeHash, string.Empty)
            .Set(r => r.ResetTokenHash, HashToken(token))
            .Set(r => r.ResetTokenExpiresAt, DateTime.UtcNow + ResetTokenLifetime);
        await _resets.UpdateOneAsync(r => r.Id == reset.Id, update);

        return new PasswordResetResult(StatusCodes.Status200OK, new PasswordResetResponseDto
        {
            Success = true,
            Message = "Code verified",
            ResetToken = token
        });
    }

    // Step 3: save the new password and close the reset
    public async Task<PasswordResetResult> ResetPasswordAsync(ResetPasswordDto request)
    {
        var email = NormaliseEmail(request.Email);
        if (string.IsNullOrEmpty(request.NewPassword) || request.NewPassword.Length < MinPasswordLength)
            return Fail(StatusCodes.Status400BadRequest, $"Password must be at least {MinPasswordLength} characters");

        if (string.IsNullOrWhiteSpace(request.ResetToken))
            return Fail(StatusCodes.Status400BadRequest, "This reset has expired. Please start again.");

        var tokenHash = HashToken(request.ResetToken);
        var reset = await _resets
            .Find(r => r.Email == email && r.ResetTokenHash == tokenHash && r.ResetTokenExpiresAt > DateTime.UtcNow)
            .FirstOrDefaultAsync();
        if (reset is null)
            return Fail(StatusCodes.Status400BadRequest, "This reset has expired. Please start again.");

        var update = Builders<User>.Update
            .Set(u => u.PasswordHash, BCrypt.Net.BCrypt.HashPassword(request.NewPassword))
            .Set(u => u.UpdatedAt, DateTime.UtcNow);
        var result = await _users.UpdateOneAsync(u => u.Id == reset.UserId, update);
        await _resets.DeleteManyAsync(r => r.Email == email);

        if (result.MatchedCount == 0)
            return Fail(StatusCodes.Status400BadRequest, "This reset has expired. Please start again.");

        return Ok("Password updated. You can sign in now.");
    }

    // E-mails are not unique in Users, so the newest account with this e-mail is used
    private async Task<User?> FindUserByEmailAsync(string email)
    {
        var pattern = new BsonRegularExpression($"^{Regex.Escape(email)}$", "i");
        return await _users.Find(Builders<User>.Filter.Regex(u => u.Email, pattern))
            .SortByDescending(u => u.CreatedAt)
            .FirstOrDefaultAsync();
    }

    private static string BuildEmailBody(string fullName, string code)
    {
        var name = string.IsNullOrWhiteSpace(fullName) ? "there" : fullName.Trim();
        return $"Hi {name},\n\n" +
               $"Your SolarGrid password reset code is {code}.\n" +
               $"It expires in {CodeLifetime.TotalMinutes:0} minutes.\n\n" +
               "If you did not ask to reset your password, you can ignore this e-mail.\n\n" +
               "SolarGrid";
    }

    private static string NormaliseEmail(string? email) => email?.Trim().ToLowerInvariant() ?? string.Empty;

    private static string HashToken(string token) =>
        Convert.ToHexString(SHA256.HashData(Encoding.UTF8.GetBytes(token)));

    private static PasswordResetResult Ok(string message) =>
        new(StatusCodes.Status200OK, new PasswordResetResponseDto { Success = true, Message = message });

    private static PasswordResetResult Fail(int statusCode, string message) =>
        new(statusCode, new PasswordResetResponseDto { Success = false, Message = message });
}
