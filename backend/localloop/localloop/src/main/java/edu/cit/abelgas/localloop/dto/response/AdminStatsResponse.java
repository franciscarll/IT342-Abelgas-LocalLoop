package edu.cit.abelgas.localloop.dto.response;

public class AdminStatsResponse {

    private long totalResidents;
    private long totalFavors;
    private long totalAnnouncements;
    private long totalReputationGiven;
    private long openFavors;
    private long claimedFavors;
    private long completedFavors;

    public AdminStatsResponse() {}

    private AdminStatsResponse(Builder b) {
        this.totalResidents       = b.totalResidents;
        this.totalFavors          = b.totalFavors;
        this.totalAnnouncements   = b.totalAnnouncements;
        this.totalReputationGiven = b.totalReputationGiven;
        this.openFavors           = b.openFavors;
        this.claimedFavors        = b.claimedFavors;
        this.completedFavors      = b.completedFavors;
    }

    public long getTotalResidents()       { return totalResidents; }
    public long getTotalFavors()          { return totalFavors; }
    public long getTotalAnnouncements()   { return totalAnnouncements; }
    public long getTotalReputationGiven() { return totalReputationGiven; }
    public long getOpenFavors()           { return openFavors; }
    public long getClaimedFavors()        { return claimedFavors; }
    public long getCompletedFavors()      { return completedFavors; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long totalResidents, totalFavors, totalAnnouncements,
                totalReputationGiven, openFavors, claimedFavors, completedFavors;

        public Builder totalResidents(long v)       { this.totalResidents = v;       return this; }
        public Builder totalFavors(long v)          { this.totalFavors = v;          return this; }
        public Builder totalAnnouncements(long v)   { this.totalAnnouncements = v;   return this; }
        public Builder totalReputationGiven(long v) { this.totalReputationGiven = v; return this; }
        public Builder openFavors(long v)           { this.openFavors = v;           return this; }
        public Builder claimedFavors(long v)        { this.claimedFavors = v;        return this; }
        public Builder completedFavors(long v)      { this.completedFavors = v;      return this; }
        public AdminStatsResponse build()           { return new AdminStatsResponse(this); }
    }
}