package demo.pharma.notification;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import demo.pharma.security.AppUser;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Query("select n from Notification n where n.recipientUser is null or n.recipientUser = :user order by n.createdAt desc")
    Page<Notification> findForUser(@Param("user") AppUser user, Pageable pageable);

    @Query("select n from Notification n where (n.recipientUser is null or n.recipientUser = :user) and n.readStatus = false order by n.createdAt desc")
    List<Notification> findUnreadForUser(@Param("user") AppUser user);
}
