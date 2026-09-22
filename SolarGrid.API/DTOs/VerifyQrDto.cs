/*
 * File: VerifyQrDto.cs
 * Description: QR scan request
 * Author: Aaron (Verification and Dashboard)
 * Date: 22/09/2026
 */
namespace SolarGrid.API.DTOs;

public class VerifyQrDto
{
    public string QrCode { get; set; } = string.Empty;
}
