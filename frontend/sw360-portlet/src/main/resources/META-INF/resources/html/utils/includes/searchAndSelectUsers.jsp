<%--
  ~ Copyright Siemens AG, 2017-2019. Part of the SW360 Portal User.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<div class="dialogs">
	<div id="search-users-div" data-title="Search Users" class="modal fade" tabindex="-1" role="dialog">
		<div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable" role="document">
		    <div class="modal-content">
			<div class="modal-body container">
                    <div id="truncationAlerter" class="alert alert-warning" role="alert" style="display: none;">
                        Output limited to 100 results. Please narrow your search.
                    </div>

                    <form>
                        <div class="row form-group">
                            <div class="col-6">
                                <input type="text" name="search" id="search-text" placeholder="Enter search text..." class="form-control" autofocus/>
                            </div>
                            <div class="col">
                                <button type="button" class="btn btn-secondary" id="search-button">Search</button>
                                <button type="button" class="btn btn-secondary" id="reset-button">Reset</button>
                            </div>
                        </div>

                        <div id="usersearchresults">
                            <div id="search-spinner" class="spinner text-center" style="display: none;">
                                <div class="spinner-border" role="status">
                                    <span class="sr-only">Loading...</span>
                                </div>
                            </div>

                            <table id="search-result-table" class="table table-bordered">
                                <colgroup>
                                    <col style="width: 1.7rem;" />
                                    <col style="width: 25;" />
                                    <col style="width: 25;" />
                                    <col style="width: 25;" />
                                    <col style="width: 25;" />
                                </colgroup>
                                <thead>
                                    <tr>
                                        <th></th>
                                        <th>Given Name</th>
                                        <th>Last Name</th>
                                        <th>Email</th>
                                        <th>Department</th>
                                    </tr>
                                </thead>
                                <tbody id="search-result-table-body">
                                </tbody>
                            </table>
                        </div>
                    </form>
				</div>
			    <div class="modal-footer">
		        <button type="button" class="btn btn-light" data-dismiss="modal">Close</button>
			        <button id="search-add-button" type="button" class="btn btn-primary">Select Users</button>
			    </div>
			</div>
		</div>
	</div>
</div>
