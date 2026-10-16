package com.smartreply.ai.config;

import com.smartreply.ai.model.Email;
import com.smartreply.ai.model.Folder;
import com.smartreply.ai.model.ReceivedEmail;
import com.smartreply.ai.model.SentEmail;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds the fictional demo mailbox (20+ messages from example.com
 * addresses) that EmailService starts with and re-seeds on reset. Kept as
 * its own class so EmailService only has to manage the collection, not
 * know how the sample data is constructed.
 */
@Component
public class DemoDataSeeder {

    public List<Email> seedEmails() {
        AtomicInteger idSeq = new AtomicInteger();
        LocalDateTime now = LocalDateTime.now();
        List<Email> emails = new ArrayList<>();

        // ---- INBOX: college assignments, meetings, events, interviews, follow-ups, customer queries ----
        emails.add(received(idSeq, "registrar@example.com", "Office of the Registrar", now.minusHours(2), Folder.INBOX,
                "Assignment Submission Reminder - Data Structures Lab 3",
                "Dear Student,\n\nThis is a reminder that Lab 3 (Binary Search Trees) is due by Friday. " +
                        "Please submit your source code and report through the portal. Late submissions " +
                        "will not be accepted without prior approval.\n\nPlease confirm once you have submitted.\n\nRegards,\nOffice of the Registrar"));

        emails.add(received(idSeq, "prof.mehta@example.com", "Prof. Mehta", now.minusHours(5), Folder.INBOX,
                "Project Meeting Tomorrow at 3 PM",
                "Hi Team,\n\nLet's meet tomorrow at 3 PM in Lab 204 to review progress on the final year " +
                        "project. Please come prepared with your module status and open blockers.\n\n" +
                        "Could you confirm your attendance?\n\nThanks,\nProf. Mehta"));

        emails.add(received(idSeq, "events@example.com", "Campus Events Committee", now.minusDays(1), Folder.INBOX,
                "Reminder: Tech Fest Registration Closes Oct 20",
                "Hello,\n\nThis is a friendly reminder that registrations for the annual Tech Fest close on " +
                        "October 20. Teams of up to four members can register through the student portal.\n\n" +
                        "Please register before the deadline to confirm your slot.\n\nSee you there!"));

        emails.add(received(idSeq, "hr.careers@example.com", "Careers Team", now.minusDays(1).minusHours(3), Folder.INBOX,
                "Interview Scheduled: Software Engineer Intern - Oct 22",
                "Dear Candidate,\n\nCongratulations! We would like to invite you for a technical interview " +
                        "for the Software Engineer Intern role on October 22 at 11 AM via video call.\n\n" +
                        "Please confirm your availability and reply with a valid ID for verification.\n\nBest,\nCareers Team"));

        emails.add(received(idSeq, "lib.services@example.com", "Library Services", now.minusDays(2), Folder.INBOX,
                "Overdue Book Notice - Immediate Action Required",
                "Dear Student,\n\nOur records show that 'Introduction to Algorithms' is overdue. This is " +
                        "urgent - please return the book immediately to avoid additional late fees.\n\n" +
                        "Kindly settle this today.\n\nLibrary Services"));

        emails.add(received(idSeq, "client.rao@example.com", "Ananya Rao", now.minusDays(2).minusHours(4), Folder.INBOX,
                "Follow-up: Invoice #1042 Payment Status",
                "Hi,\n\nFollowing up on invoice #1042 sent last week. Could you confirm whether the payment " +
                        "has been processed on your end? We would appreciate an update by Thursday.\n\n" +
                        "Thanks,\nAnanya"));

        emails.add(received(idSeq, "support.customer@example.com", "Customer Support Desk", now.minusDays(3), Folder.INBOX,
                "Customer Query: Unable to Reset Account Password",
                "Hello Support,\n\nI am unable to reset my account password - the reset link in the email " +
                        "keeps expiring. Could you please help resolve this as soon as possible?\n\n" +
                        "Thank you,\nCustomer Support Desk (on behalf of end user)"));

        emails.add(received(idSeq, "coordinator.internship@example.com", "Internship Coordinator", now.minusDays(3).minusHours(6), Folder.INBOX,
                "Action Required: Submit Internship Completion Certificate",
                "Dear Student,\n\nPlease submit your internship completion certificate and final report by " +
                        "next Friday. This is required to process your credits for this semester.\n\n" +
                        "Let us know if you need an extension.\n\nRegards,\nInternship Coordinator"));

        emails.add(received(idSeq, "alumni.network@example.com", "Alumni Network", now.minusDays(4), Folder.INBOX,
                "Invitation: Alumni Mentorship Meetup Next Week",
                "Hi,\n\nYou're invited to our Alumni Mentorship Meetup next week. It's a great chance to " +
                        "connect with seniors working in the industry.\n\nPlease RSVP if you plan to attend.\n\n" +
                        "Cheers,\nAlumni Network"));

        emails.add(received(idSeq, "exam.cell@example.com", "Examination Cell", now.minusDays(5), Folder.INBOX,
                "Final Exam Hall Ticket - Download Before Oct 25",
                "Dear Student,\n\nYour hall ticket for the upcoming final examinations is now available. " +
                        "Please download and print it before October 25.\n\nContact us if you face any issues.\n\n" +
                        "Examination Cell"));

        // ---- SPAM ----
        emails.add(received(idSeq, "promo@example.com", "MegaDeals Promotions", now.minusDays(1), Folder.SPAM,
                "You Won a Free Prize!!! Claim NOW",
                "CONGRATULATIONS!!! You have been selected to WIN a free prize worth $1000. Click the link " +
                        "immediately and claim before it expires. Limited time offer!!!"));

        emails.add(received(idSeq, "lottery.winner@example.com", "International Lottery Board", now.minusDays(2), Folder.SPAM,
                "URGENT: Your Lottery Winning Notification",
                "Dear Winner, your email has won our international lottery draw. To claim your prize " +
                        "immediately, send your bank details and a processing fee today."));

        emails.add(received(idSeq, "offers.deals@example.com", "SuperSaver Offers", now.minusDays(3), Folder.SPAM,
                "90% OFF Everything - Today Only",
                "Massive discounts on electronics, fashion and more. This deal expires tonight - act fast " +
                        "and click now before stocks run out!"));

        emails.add(received(idSeq, "crypto.invest@example.com", "Quick Crypto Returns", now.minusDays(4), Folder.SPAM,
                "Double Your Investment in 24 Hours",
                "Invest now and double your money in just 24 hours, guaranteed! Limited slots available, " +
                        "act immediately to secure your spot."));

        // ---- SENT ----
        emails.add(sent(idSeq, "demo@example.com", "to.manager@example.com", now.minusDays(1), Folder.SENT, true,
                "Re: Weekly Status Report",
                "Hi,\n\nPlease find this week's status update attached. All assigned tasks are on track " +
                        "and the sprint review is scheduled for Friday.\n\nBest,\nDemo User"));

        emails.add(sent(idSeq, "demo@example.com", "prof.mehta@example.com", now.minusDays(2), Folder.SENT, true,
                "Confirming Attendance for Project Meeting",
                "Hi Prof. Mehta,\n\nConfirming that I will attend tomorrow's 3 PM project meeting in Lab 204.\n\n" +
                        "Regards,\nDemo User"));

        emails.add(sent(idSeq, "demo@example.com", "hr.careers@example.com", now.minusDays(3), Folder.SENT, true,
                "Re: Interview Scheduled: Software Engineer Intern",
                "Dear Careers Team,\n\nThank you for the opportunity. I confirm my availability for the " +
                        "interview on October 22 at 11 AM.\n\nBest regards,\nDemo User"));

        emails.add(sent(idSeq, "demo@example.com", "client.rao@example.com", now.minusDays(4), Folder.SENT, true,
                "Re: Invoice #1042 Payment Status",
                "Hi Ananya,\n\nThe payment for invoice #1042 was processed yesterday. Please allow 2-3 " +
                        "business days for it to reflect.\n\nThanks,\nDemo User"));

        // ---- DRAFTS ----
        emails.add(sent(idSeq, "demo@example.com", "lib.services@example.com", now.minusHours(6), Folder.DRAFTS, false,
                "Re: Overdue Book Notice",
                "Hi,\n\nApologies for the delay, I will return the book "));

        emails.add(sent(idSeq, "demo@example.com", "coordinator.internship@example.com", now.minusDays(1).minusHours(2), Folder.DRAFTS, false,
                "Re: Internship Completion Certificate",
                "Dear Coordinator,\n\nI am currently collecting the signed certificate from my supervisor and "));

        emails.add(sent(idSeq, "demo@example.com", "events@example.com", now.minusDays(2).minusHours(1), Folder.DRAFTS, false,
                "Re: Tech Fest Registration",
                "Hi team,\n\nWe would like to register a team of four for the Tech Fest. Our team name is "));

        return emails;
    }

    private Email received(AtomicInteger idSeq, String fromAddress, String fromName, LocalDateTime date,
                            Folder folder, String subject, String body) {
        return new ReceivedEmail("e" + idSeq.incrementAndGet(), fromAddress, fromName,
                "demo@example.com", subject, body, date, folder);
    }

    private Email sent(AtomicInteger idSeq, String fromAddress, String toAddress, LocalDateTime date,
                        Folder folder, boolean isSent, String subject, String body) {
        return new SentEmail("e" + idSeq.incrementAndGet(), fromAddress, "Demo User", toAddress,
                subject, body, date, folder, isSent);
    }
}
