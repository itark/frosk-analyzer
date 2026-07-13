package nu.itark.frosk.repo;

import nu.itark.frosk.model.CryptoPaperAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CryptoPaperAccountRepository extends JpaRepository<CryptoPaperAccount, Long> {
}
