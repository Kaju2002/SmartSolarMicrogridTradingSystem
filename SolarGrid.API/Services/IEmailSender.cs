/*
 * File: IEmailSender.cs
 * Description: Outgoing e-mail contract
 * Author: Vithusha (Identity and Access)
 * Date: 04/10/2026
 */
namespace SolarGrid.API.Services;

public interface IEmailSender
{
    // False until SMTP host, username and password are set
    bool IsConfigured { get; }

    // Send one plain-text e-mail
    Task SendAsync(string toAddress, string subject, string body);
}
