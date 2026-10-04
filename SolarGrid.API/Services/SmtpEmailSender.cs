/*
 * File: SmtpEmailSender.cs
 * Description: Sends e-mail over SMTP (e.g. Gmail with an app password).
 *              The password comes from user-secrets or the IIS app pool, never appsettings.
 * Author: Vithusha (Identity and Access)
 * Date: 04/10/2026
 */
using System.Net;
using System.Net.Mail;

namespace SolarGrid.API.Services;

public class SmtpEmailSender : IEmailSender
{
    private readonly string _host;
    private readonly int _port;
    private readonly string _username;
    private readonly string _password;
    private readonly string _fromAddress;
    private readonly string _fromName;

    // Read EmailSettings; the sender address defaults to the SMTP username
    public SmtpEmailSender(IConfiguration configuration)
    {
        var section = configuration.GetSection("EmailSettings");
        _host = section["Host"] ?? string.Empty;
        _port = int.TryParse(section["Port"], out var port) ? port : 587;
        _username = section["Username"] ?? string.Empty;
        _password = section["Password"] ?? string.Empty;
        _fromAddress = string.IsNullOrWhiteSpace(section["FromAddress"]) ? _username : section["FromAddress"]!;
        _fromName = section["FromName"] ?? "SolarGrid";
    }

    public bool IsConfigured =>
        !string.IsNullOrWhiteSpace(_host)
        && !string.IsNullOrWhiteSpace(_username)
        && !string.IsNullOrWhiteSpace(_password);

    // STARTTLS on port 587
    public async Task SendAsync(string toAddress, string subject, string body)
    {
        using var message = new MailMessage
        {
            From = new MailAddress(_fromAddress, _fromName),
            Subject = subject,
            Body = body,
            IsBodyHtml = false
        };
        message.To.Add(toAddress);

        using var client = new SmtpClient(_host, _port)
        {
            EnableSsl = true,
            Credentials = new NetworkCredential(_username, _password)
        };
        await client.SendMailAsync(message);
    }
}
