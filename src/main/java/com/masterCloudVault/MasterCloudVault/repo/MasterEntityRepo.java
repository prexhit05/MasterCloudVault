package com.masterCloudVault.MasterCloudVault.repo;

import com.masterCloudVault.MasterCloudVault.entity.MasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MasterEntityRepo extends JpaRepository<MasterEntity,Integer> {

}
