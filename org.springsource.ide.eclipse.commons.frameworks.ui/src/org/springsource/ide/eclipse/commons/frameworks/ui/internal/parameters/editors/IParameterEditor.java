/*******************************************************************************
 * Copyright (c) 2012 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.commons.frameworks.ui.internal.parameters.editors;

import org.eclipse.swt.widgets.Composite;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameter;
import org.springsource.ide.eclipse.commons.frameworks.core.internal.commands.ICommandParameterDescriptor;


/**
 * UI controls for a parameter instance, where parameter values can be displayed
 * and changed.
 * <p>
 * Implementors need NOT include a label control with the parameter name as part
 * of its UI controls. Instead, it should answer a call to whether the creator
 * of the editor should include the parameter name as a label or not. It is
 * expected that UI controls for the parameter be simple and be arranged in the
 * same row as the label of the parameter.
 * </p>
 * @author Nieraj Singh
 */
public interface IParameterEditor {

	/**
	 * Returns the command parameter descriptor that describes the properties of
	 * the parameter. Should not be null.
	 * 
	 * @return non-null parameter descriptor.
	 */
	public ICommandParameterDescriptor getParameterDescriptor();

	/**
	 * Returns the command parameter instance used by the editor. This contains
	 * the values set by the editor.
	 * 
	 * @return command parameter instance used by the editor. Should not be
	 *        null.
	 */
	public ICommandParameter getParameter();

	/**
	 * Creates the editor controls, and returns the composite containing the
	 * created editor controls, or null if nothing is created.
	 * <p>
	 * It is NOT necessary to include the parameter name as a label control, as
	 * this is generated by the component that creates the editor.
	 * </p>
	 * 
	 * @param parent
	 *           for the editor controls
	 * @return Composite containing editor controls, or null if nothing created
	 */
	public Composite createControls(Composite parent);

	/**
	 * True if the parameter name should be included as a label control. NOTE
	 * that implementors need not include the parameter name as a label. The
	 * wizard or dialogue that creates the editors is responsible for creating
	 * parameter name labels.
	 * 
	 * @return true if the parameter name should be included as a label control.
	 *        False otherwise.
	 */
	public boolean requiresParameterNameLabel();

	public IUIChangeListener addUIChangeListener(IUIChangeListener listener);

	public IUIChangeListener removeUIChangeListener(IUIChangeListener listener);

}
