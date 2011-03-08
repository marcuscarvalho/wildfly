/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.processors;

import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.Indexer;
import org.junit.Test;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TransactionManagementAnnotationProcessorTestCase {
    @TransactionManagement(TransactionManagementType.BEAN)
    private static class MyBean {

    }

    private static class SubBean extends MyBean {

    }

    private static void index(Indexer indexer, Class<?> cls) throws IOException {
        InputStream stream = cls.getClassLoader().getResourceAsStream(cls.getName().replace('.', '/') + ".class");
        try {
            indexer.index(stream);
        }
        finally {
            stream.close();
        }
    }

    @Test
    public void test1() throws Exception {
        DeploymentUnit deploymentUnit = null;
        DeploymentPhaseContext phaseContext = null;
        Indexer indexer = new Indexer();
        index(indexer, MyBean.class);
        index(indexer, SubBean.class);
        CompositeIndex index = new CompositeIndex(Arrays.asList(indexer.complete()));

        EJBComponentDescription componentDescription = new EJBComponentDescription(MyBean.class.getSimpleName(), MyBean.class.getName(), "TestModule", "TestApp");
        TransactionManagementAnnotationProcessor processor = new TransactionManagementAnnotationProcessor();
        processor.processComponentConfig(deploymentUnit, phaseContext, index, componentDescription);

        assertEquals(TransactionManagementType.BEAN, componentDescription.getTransactionManagementType());
    }

    /**
     * EJB 3.1 FR 13.3.1, the default transaction management type is container-managed transaction demarcation.
     */
    @Test
    public void testDefault() {
        EJBComponentDescription componentDescription = new EJBComponentDescription("TestBean", "TestClass", "TestModule", "TestApp");
        assertEquals(TransactionManagementType.CONTAINER, componentDescription.getTransactionManagementType());
    }

    /**
     * EJB 3.1 FR 13.3.6 The TransactionManagement annotation is applied to the enterprise bean class.
     */
    @Test
    public void testSubClass() throws Exception {
        DeploymentUnit deploymentUnit = null;
        DeploymentPhaseContext phaseContext = null;
        Indexer indexer = new Indexer();
        index(indexer, MyBean.class);
        index(indexer, SubBean.class);
        CompositeIndex index = new CompositeIndex(Arrays.asList(indexer.complete()));

        EJBComponentDescription componentDescription = new EJBComponentDescription(SubBean.class.getSimpleName(), SubBean.class.getName(), "TestModule", "TestApp");
        TransactionManagementAnnotationProcessor processor = new TransactionManagementAnnotationProcessor();
        processor.processComponentConfig(deploymentUnit, phaseContext, index, componentDescription);

        assertEquals(TransactionManagementType.CONTAINER, componentDescription.getTransactionManagementType());
    }
}
