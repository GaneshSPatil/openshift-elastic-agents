/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cd.go.contrib.elasticagent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

class SetupSemaphore implements Runnable {
    private final Integer maxAllowedPendingPods;
    private final Map<String, OpenshiftInstance> instances;
    private final Semaphore semaphore;

    public SetupSemaphore(Integer maxAllowedPendingPods, Map<String, OpenshiftInstance> instances, Semaphore semaphore) {
        this.maxAllowedPendingPods = maxAllowedPendingPods;
        this.instances = instances;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        List<OpenshiftInstance> pendingInstances = getPendingInstances(instances);
        int totalPendingPods = pendingInstances.size();
        int availablePermits = maxAllowedPendingPods - totalPendingPods;

        if (availablePermits <= 0) {
            // no more capacity available.
            semaphore.drainPermits();
        } else {
            int semaphoreValueDifference = availablePermits - semaphore.availablePermits();
            if (semaphoreValueDifference > 0) {
                semaphore.release(semaphoreValueDifference);
            } else if (semaphoreValueDifference < 0) {
                semaphore.tryAcquire(Math.abs(semaphoreValueDifference));
            }
        }
    }

    private List<OpenshiftInstance> getPendingInstances(Map<String, OpenshiftInstance> instances) {
        ArrayList<OpenshiftInstance> pendingInstances = new ArrayList<>();
        for (OpenshiftInstance openshiftInstance : instances.values()) {
            if (openshiftInstance.isPending()) {
                pendingInstances.add(openshiftInstance);
            }
        }

        return pendingInstances;
    }
}
