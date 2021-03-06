/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package demo.throttling.server;

import com.codahale.metrics.MetricRegistry;

import org.apache.cxf.management.codahale.CodahaleMetricsContext;
import org.apache.cxf.management.codahale.MetricsContext;
import org.apache.cxf.message.Message;

/**
 * 
 */
public abstract class Customer {
    protected final String name;
    protected volatile CodahaleMetricsContext metrics;
    
    public Customer(String n) {
        name = n;
    }
    
    MetricsContext getMetricsContext(MetricRegistry registry) {
        if (metrics == null) {
            metrics = new CodahaleMetricsContext("Customer-" + name, registry);
        }
        return metrics;
    }

    public abstract long throttle(Message m);
    
    
    public static class PremiumCustomer extends Customer {
        public PremiumCustomer(String n) {
            super(n);
        }
        public long throttle(Message m) {
            //Premium customers are unthrottled
            return 0;
        }
    }
    public static class PreferredCustomer extends Customer {
        public PreferredCustomer(String n) {
            super(n);
        }
        public long throttle(Message m) {
            //System.out.println("p  " + metrics.getTotals().getOneMinuteRate() + "  " + metrics.getTotals().getCount());
            //Preferred customers are unthrottled until they hit 100req/sec, then start delaying by .05 seconds
            //(drops to max of 50req/sec until below the 100req/sec rate)
            if (metrics.getTotals().getOneMinuteRate() > 100) {
                return 20;
            }
            return 0;
        }
    }
    public static class RegularCustomer extends Customer {
        public RegularCustomer(String n) {
            super(n);
        }
        public long throttle(Message m) {
            //Regular customers are unthrottled until they hit 25req/sec, then start delaying by 0.25 seconds 
            //(drops to max of 4req/sec until below the 25req/sec rate)
            if (metrics.getTotals().getOneMinuteRate() > 25) {
                return 250;
            }
            //They also get throttled more if they are over 10req/sec over a 5 minute period  
            //(drops to max of 2req/sec until below the 10req/sec rate)
            if (metrics.getTotals().getFiveMinuteRate() > 10) {
                return 500;
            }
            return 0;
        }
    }
    public static class CheapCustomer extends Customer {
        public CheapCustomer(String n) {
            super(n);
        }
        public long throttle(Message m) {
            //System.out.println("ch  " + metrics.getTotals().getOneMinuteRate() + "  " + metrics.getTotals().getCount());
            //Cheap customers are always get a .1 sec delay
            long delay = 100;
            //Then they get futher throttled dependending on rates
            if (metrics.getTotals().getOneMinuteRate() > 5) {
                delay += 1000;
            }
            //They also get throttled after 5 minutes of more than 
            if (metrics.getTotals().getFiveMinuteRate() > 1) {
                delay += 1000;
            }
            return delay;
        }
    }

}
