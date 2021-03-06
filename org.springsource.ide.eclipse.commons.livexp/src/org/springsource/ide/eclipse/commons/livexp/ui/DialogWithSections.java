/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springsource.ide.eclipse.commons.livexp.ui;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;
import org.springsource.ide.eclipse.commons.frameworks.core.ExceptionUtil;
import org.springsource.ide.eclipse.commons.livexp.Activator;
import org.springsource.ide.eclipse.commons.livexp.core.CompositeValidator;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.livexp.core.Validator;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;

/**
 * Single page dialog with status bar and ok cancel buttons capable of hosting
 * {@link PageSection}s.
 *
 * @author Kris De Volder
 */
public abstract class DialogWithSections
	extends TitleAreaDialog
	implements ValueListener<ValidationResult>, IPageWithSections, IPageWithOkButton
{

	private String title;
	private final OkButtonHandler model;

	public DialogWithSections(String title, OkButtonHandler model, Shell shell) {
		super(shell);
		this.title = title;
		this.model = model;
	}

	public void create() {
		super.create();
		setTitle(title);
	}

	protected Control createDialogArea(Composite parent) {
//		readSettings();
		Composite page = (Composite) super.createDialogArea(parent);

//		GridDataFactory.fillDefaults().grab(true,true).applyTo(parent);
//		Composite page = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 12;
        layout.marginWidth = 12;
        page.setLayout(layout);
        validator = new CompositeValidator();
        for (PageSection section : getSections()) {
			section.createContents(page);
			validator.addChild(section.getValidator());
		}
        validator.addListener(this);
        applyDialogFont(page);
        return page;
	}

	/**
	 * A delay used for posting status messages to the dialog area after a status update happens.
	 * This is to get rid of spurious message that only appear for a fraction of a second as
	 * some auto updating states in models are inconsistent. E.g. in new boot project wizard
	 * when project name is entered it is temporarily inconsistent with default project location until
	 * that project location itself is updated in response to the change event from the project name.
	 * If the project location validator runs before the location update, a spurious validation error
	 * temporarily results.
	 *
	 * Note: this is a hacky solution. It would be better if the LiveExp framework solved this by
	 * tracking and scheduling refreshes based on the depedency graph. Thus it might guarantee
	 * that the validator never sees the inconsistent state because it is refreshed last.
	 */
	private static final long MESSAGE_DELAY = 250;


	private List<WizardPageSection> sections = null;
	private CompositeValidator validator;
	private UIJob updateJob;

	protected synchronized List<WizardPageSection> getSections() {
		if (sections==null) {
			sections = safeCreateSections();
		}
		return sections;
	}

	private List<WizardPageSection> safeCreateSections() {
		try {
			return createSections();
		} catch (CoreException e) {
			Activator.log(e);
			return Arrays.asList(
					new CommentSection(this, "Dialog couldn't be created because of an unexpected error:+\n"+ExceptionUtil.getMessage(e)+"\n\n"
							+ "Check the error log for details"),
					new ValidatorSection(Validator.alwaysError(ExceptionUtil.getMessage(e)), this)
			);
		}
	}

	/**
	 * This method should be implemented to generate the contents of the page.
	 */
	protected List<WizardPageSection> createSections() throws CoreException {
		//This default implementation is meant to be overridden
		return Arrays.asList(
				new CommentSection(this, "Override DialogWithSections.createSections() to provide real content."),
				new ValidatorSection(Validator.alwaysError("Subclass must implement validation logic"), this)
		);
	}

	/**
	 * Convert ValidationResult into an Eclipse IStatus object.
	 */
	private IStatus toStatus(ValidationResult value) {
		if (value==null || value.isOk()) {
			return Status.OK_STATUS;
		} else {
			return new Status(value.status, Activator.PLUGIN_ID , value.msg);
		}
	}

	public void gotValue(LiveExpression<ValidationResult> exp, final ValidationResult status) {
		scheduleUpdateJob();
	}

	private synchronized void scheduleUpdateJob() {
		Shell shell = getShell();
		if (shell!=null) {
			if (this.updateJob==null) {
				this.updateJob = new UIJob("Update Wizard message") {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						updateStatus(validator.getValue());
//						IStatus status = toStatus(validator.getValue());
//						updateStatus(status);
						return Status.OK_STATUS;
					}
				};
				updateJob.setSystem(true);
			}
			updateJob.schedule(MESSAGE_DELAY);
		}
	}

	private void updateStatus(ValidationResult status) {
		boolean enableOk = true;
		if (status==null || status.isOk()) {
			setMessage("", IMessageProvider.NONE);
		} else {
			setMessage(status.msg, status.getMessageProviderStatus());
			enableOk = status.status<IStatus.ERROR;
		}
		Button okButton = getButton(IDialogConstants.OK_ID);
		if (okButton!=null) {
			okButton.setEnabled(enableOk);
		}
	}

	public void dispose() {
		for (WizardPageSection s : sections) {
			s.dispose();
		}
	}

	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}

	@Override
	protected final void okPressed() {
		super.okPressed();
		try {
			model.performOk();
		} catch (Exception e) {
			Activator.log(e);
			MessageDialog.openError(getShell(), "Error", ExceptionUtil.getMessage(e));
		}
	}

	/**
	 * Simulate clicking the ok button. Does nothing if ok button is not found or disabled.
	 */
	public boolean clickOk() {
		Button button = getButton(OK);
		if (button!=null && button.isEnabled()) {
			okPressed();
			return true;
		}
		return false;
	}


	@Override
	public IRunnableContext getRunnableContext() {
		return PlatformUI.getWorkbench().getProgressService();
	}
}
