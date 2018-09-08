/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server.tcc;

import com.google.common.collect.Sets;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.FinishedEvent;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.FinishedEventRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipateEventRepository;
import org.apache.servicecomb.saga.alpha.server.tcc.jpa.ParticipatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("rdbTransactionEventService")
public class RdbTransactionEventService implements TransactionEventService {

  @Autowired
  private ParticipateEventRepository participateEventRepository;

  @Autowired
  private FinishedEventRepository finishedEventRepository;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public void addEvent(ParticipatedEvent participateEvent) {
    participateEventRepository.save(participateEvent);
  }

  @Override
  public Set<ParticipatedEvent> getEventByGlobalTxId(String globalTxId) {
    Optional<List<ParticipatedEvent>> list = participateEventRepository.findByGlobalTxId(globalTxId);
    return list.map(Sets::newHashSet).orElseGet(Sets::newHashSet);
  }

  @Override
  @Transactional
  public void migration(String globalTxId, String localTxId) {
    participateEventRepository.findByGlobalTxIdAndLocalTxId(globalTxId, localTxId).ifPresent( e -> {
      participateEventRepository.delete(e.getId());
      FinishedEvent finishedEvent = new FinishedEvent(
          e.getGlobalTxId(),
          e.getLocalTxId(),
          e.getParentTxId(),
          e.getServiceName(),
          e.getInstanceId(),
          e.getConfirmMethod(),
          e.getCancelMethod(),
          e.getStatus()
      );
      finishedEventRepository.save(finishedEvent);
    });
  }
}
