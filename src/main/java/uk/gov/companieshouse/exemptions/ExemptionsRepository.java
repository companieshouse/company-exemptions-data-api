package uk.gov.companieshouse.exemptions;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExemptionsRepository extends MongoRepository<CompanyExemptionsDocument, String> {

    @Query("{'_id': ?0, 'updated.at':{$gte : { \"$date\" : \"?1\" } }}")
    List<CompanyExemptionsDocument> findUpdatedExemptions(String companyNumber, String at);
}
