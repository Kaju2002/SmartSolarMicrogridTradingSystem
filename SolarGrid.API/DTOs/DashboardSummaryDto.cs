/*
 * File: DashboardSummaryDto.cs
 * Description: Dashboard booking counts
 * Author: Aaron (Verification and Dashboard)
 * Date: 22/09/2026
 */
namespace SolarGrid.API.DTOs;

public class DashboardSummaryDto
{
    public int PendingCount { get; set; }
    public int ApprovedCount { get; set; }
    public int CompletedCount { get; set; }
}
