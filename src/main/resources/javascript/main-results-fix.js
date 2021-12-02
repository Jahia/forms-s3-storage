PageableResultsView = Backbone.View.extend({
    initialize: function (options) {
        //Formats export value
        var self = this;
        this.formatter = function(type) {
            function decode(string) {
                var parser = new DOMParser;
                var dom = parser.parseFromString(
                    '<!doctype html><body>' + string,
                    'text/html');
                return dom.body.textContent;
            }
            return {
                type : type,
                body: function (data, row, column, node) {
                    console.log("formater data: ",data);
                    if (typeof data === "string") {
                        // rendered source path
                        if (data.indexOf("href") !== -1){
                            const parser = new DOMParser();
                            const el = parser.parseFromString(data, "text/html");
                            const links = [...el.getElementsByTagName( 'a' )]
                                .map(a=>a.href)
                                .reduce((str,link,index,array)=>{
                                    if (!link.startsWith("http")) {
                                        link = origin + link;
                                    }
                                    str += link;
                                    if(index < array.length-1)
                                        str +=";";
                                    return str
                                },"");

                            return links;

                            // //second group retrieves href value
                            // let groupArray = data.match(/<a\s+(?:[^>]*?\s+)?href=(["'])(.*?)\1/);
                            // let link = groupArray[2];
                            // //if URL is relative
                            // if (!link.startsWith("http")) {
                            // // if (!link.startsWith(origin)) {
                            //     return origin + link;
                            // }
                            // return link;
                        } else if (data.indexOf("<input type='checkbox'") !== -1) {
                            return '';
                        }
                        // Count rating icons
                        if (data.indexOf("rated")  !== -1) {
                            return data.split("<span>").length - 1;
                        } else if (data.indexOf("numberOn") !== -1) {
                            var delimiter = "<p>";
                            startIndex = data.indexOf(delimiter) + delimiter.length;
                            endIndex = data.indexOf("</p>");
                            return data.substring(startIndex,endIndex);
                        }
                        //Get value for switch
                        if (data.indexOf("fa-toggle-on") !== -1) {
                            return "ON";
                        }
                        else if (data.indexOf("fa-toggle-off") !== -1) {
                            return "OFF";
                        }
                        //Get value for country
                        if (data.indexOf("class=\"flag-icon") !== -1) {
                            var name = data.match(/i>.*<\//)[0];
                            return name.substring(8, name.length - 2);
                        }
                        //Process matrix fields
                        if (data.indexOf("class=\"dl-horizontal\">") !== -1) {
                            var object = $(data)[0];
                            console.log("FORMS object: ",object);
                            var result = this.type === "excel" ? "=" : "";
                            for (var i = 0; i < object.children.length; i++) {
                                var child = object.children[i];
                                if (child.tagName === "DT") {
                                    result += this.type === "excel" ?  "\"" + child.innerText + ": ": child.innerText + ": "
                                }
                                else if (child.tagName === "DD") {
                                    if (child.firstElementChild != null) {
                                        var lis = child.firstElementChild.children;
                                        for (var j = 0; j < lis.length; j++) {
                                            result += lis[j].innerText;
                                            if (j !== lis.length - 1) {
                                                result += ", ";
                                            }
                                        }
                                    } else {
                                        result += child.innerText;
                                    }
                                    switch (this.type) {
                                        case "print" : result += "<br>";
                                            break;
                                        case "csv" : result += ";";
                                            break;
                                        case "excel" :
                                            if (i !== object.children.length - 1) {
                                                result += "\"& CHAR( 10 ) &";
                                            }
                                            else {
                                                result += "\"";
                                            }
                                            break;
                                        default: result += "\n";
                                    }
                                }
                            }
                            return decode(result);
                        }
                    }
                    return decode(data);
                }
            }
        };
        this.grid_el    = options.grid_el;
        this.collection = options.collection;
        this.listenTo(this.collection, 'sync', this.render);
        this.rowsSelected = {};

        getLabelsFromResults(labels, this.collection.formId);
        var formName = null;
        getFormName(this.collection.formId).done(function(data){
            formName = data.success;
        });
        var columns = [];
        //Displaying only 5 first columns
        _.each(labels, function (labelObject, index) {
            var columnTitle = (labelObject.label) ? labelObject.label : labelObject.htmlId;
            if (index < 6 && index > 0) {
                columns.push({title: columnTitle, orderable: false, name: labelObject.htmlId, requiresPermissions: false});
            } else {
                columns.push({title: columnTitle, visible: false, orderable: false, name: labelObject.htmlId, requiresPermissions: columnTitle === '<input type="checkbox">'});
            }
        });

        //find selected rows
        $('#formDataTable').on('click', 'input[type="checkbox"]', function() {
            var jcrId = $(this).attr('data-jcrid');
            self.rowsSelected[jcrId].isSelected = !self.rowsSelected[jcrId].isSelected;
        });

        $('#formDataTable').on( 'init.dt', function () {
            //Add event listener for data table head
            $('.dataTables_scrollHead').on('click', 'input[type="checkbox"]', function() {
                for (var i in self.rowsSelected) {
                    if (self.rowsSelected[i].el === undefined) {
                        self.rowsSelected[i].el = $('input[data-jcrid="' + i + '"]');
                    }
                    self.rowsSelected[i].el.prop('checked',  $(this).is(":checked"));
                    self.rowsSelected[i].isSelected = $(this).is(":checked");
                }
            });
        });

        self.permissions = false;
        $.ajax({
            url: getFetchURL({
                siteId       : self.collection.site,
                apiMethodName: 'results_getpermissions'
            }),
            type: 'GET',
            contentType: 'application/json',
            dataType: 'json',
            success: function (result) {
                self.permissions = result.permissions;
                initializeDataTable();
            }
        });

        function initializeDataTable() {
            if (self.permissions) {
                columns[0].visible = true;
            } else {
                columns.splice(0,1);
            }
            self.datatable = self.grid_el.DataTable({
                columns       : columns,
                scrollY       : '55vh',
                border        : false,
                serverSide    : false,
                deferRender   : true,
                processing    : false,
                responsive    : {details: false},
                scrollCollapse: false,
                paging        : true,
                scrollX       : true,
                scroller      : true,
                autoWidth     : true,
                orderFixed    : [1, 'desc'],
                ordering      : false,
                select        : true,
                dom           : "<'row'<'col-sm-6'l><'col-sm-3'i><'col-sm-3'f>><'row'<'col-sm-12'tr>>",
                buttons       : [
                    {
                        extend  : 'copyHtml5',
                        text    : options.copyLabel,
                        exportOptions: {
                            columns: ':visible',
                            format: self.formatter("copy")
                        }
                    },
                    {
                        extend : 'csv',
                        exportOptions: {
                            columns: ':visible',
                            format: self.formatter("csv")
                        },
                        title: formName
                    },
                    {
                        extend : 'excelHtml5',
                        exportOptions: {
                            columns: ':visible',
                            format: self.formatter("excel")
                        },
                        title: formName
                    },
                    {
                        extend     : 'pdfHtml5',
                        orientation: 'landscape',
                        pageSize   : 'TABLOID',
                        exportOptions: {
                            columns: ':visible',
                            format: self.formatter("pdf")
                        },
                        title: formName
                    },
                    {
                        extend  : 'print',
                        text    : options.printLabel,
                        exportOptions: {
                            columns: ':visible',
                            format: self.formatter("print")
                        }
                    },
                    {
                        extend    : 'collection',
                        text      : options.showHideColumnsLabel,
                        buttons   : function() {
                            var collectionOfButtons = [];
                            var selectBtnClass = '';
                            var initSelectedBtns = false;
                            for (var i in columns) {
                                if (i < 6 && i !== 0) {
                                    initSelectedBtns = true;
                                    selectBtnClass = 'selected';
                                } else {
                                    initSelectedBtns = false;
                                    selectBtnClass = '';
                                }
                                if(columns[i].title !== '<input type="checkbox">') {
                                    collectionOfButtons.push({
                                        text: columns[i].title,
                                        action: showHideButtonAction,
                                        selected: initSelectedBtns,
                                        title: columns[i].title,
                                        requiresPermissions: columns[i].requiresPermissions,
                                        className: selectBtnClass,
                                        name: columns[i].name
                                    });
                                }
                            }
                            function showHideButtonAction (e, dt, node, config) {
                                if (config.requiresPermissions && !self.permissions) {
                                    return;
                                }
                                var column = self.datatable.column(config.name + ':name');
                                column.visible(!column.visible(), false);
                                if (column.visible()) {
                                    node.addClass('selected');
                                } else {
                                    node.removeClass('selected');
                                }
                            }
                            return collectionOfButtons;
                        },
                        visibility: true
                    },
                    {
                        // extend: 'colvisGroup',
                        text: options.showAllColumnsLabel,
                        action: function(e, dt, node, config) {
                            self.datatable.columns().every( function (index) {
                                if (self.permissions) {
                                    if (index !== 0) {
                                        this.visible(true, false);
                                    }
                                } else {
                                    this.visible(true, false);
                                }
                                var selector = self.datatable.buttons(columns[index].name + ':name')[0];
                                if (selector !== undefined) {
                                    $(selector.node).addClass('selected');
                                }
                            });
                        }
                    },
                    {
                        text: 'Delete',
                        action: function() {
                            var confirmation = confirm("Are you sure you want to delete the selected record(s)?");
                            if (confirmation) {
                                //Get the selected rows
                                var selectedRows = [];
                                for (var i in self.rowsSelected) {
                                    if (self.rowsSelected[i].isSelected) {
                                        selectedRows.push(i);
                                    }
                                }
                                if (selectedRows.length === 0) {
                                    return;
                                }
                                if (self.permissions) {
                                    $.ajax({
                                        url: getFetchURL({
                                            formId       : self.collection.formId,
                                            apiMethodName: 'results_removeselectedsubmissions'
                                        }),
                                        type: 'POST',
                                        contentType: 'application/json',
                                        dataType: 'json',
                                        data: JSON.stringify({
                                            submissionIds: selectedRows
                                        }),
                                        success: function (response) {
                                        },
                                        error: function (response) {
                                            if (response.status === 403) {
                                                alert("You have insufficient permissions to delete the result")
                                            }
                                        }
                                    });
                                    setTimeout(function() {
                                        //Use timeout to make sure enough time is given for request to be made. (IE11)
                                        window.location.reload();
                                    }, 500);
                                }
                            }
                        },
                        className: !self.permissions ? 'hidden' : '',
                        enabled: false
                    }
                ],
                language      : {
                    search    : options.searchLabel + '&nbsp;&nbsp;',
                    info      : options.resultsFromLabel + ' _START_ ' + options.resultsToLabel + ' _END_',
                    emptyTable: formsResultLabels.isResultsViewer ? options.noResultLabel : formsResultLabels.noPermissionForForm // Here
                }
            });

            var checkedRows = 0;
            $('.dataTables_scrollHead').on('click', 'input[type="checkbox"]', function() {
                if (document.querySelectorAll('input[type="checkbox"]:checked')[0]) {
                    for (var i = 1; i < document.querySelectorAll('input[type="checkbox"]:checked').length; i++) {
                        checkedRows += 1;
                    }
                    checkedRows > 0 ? self.datatable.button(7).enable(true) : self.datatable.button(7).enable(false);
                } else {
                    checkedRows = document.querySelectorAll('input[type="checkbox"]:checked').length;
                    checkedRows > 0 ? self.datatable.button(7).enable(true) : self.datatable.button(7).enable(false);
                }
            });
            $('#formDataTable').on('click', 'input[type="checkbox"]', function() {
                this.checked ? checkedRows++ : checkedRows--;
                checkedRows > 0 ? self.datatable.button(7).enable(true) : self.datatable.button(7).enable(false);
            });

            self.datatable.buttons().container().appendTo('#formDataTable_wrapper .col-sm-6:eq(0)');
            var gridEl = self.grid_el;
            gridEl.on('column-visibility.dt', function (e, settings, column, state) {
                gridEl.DataTable().scroller.measure();
            });
        }
    },

    render    : function () {
        if (this.collection.resetted) {
            this.datatable.clear().draw(true);
            this.collection.resetted = false;
        }
        if (this.collection.length > 0) {
            var rows = this.collection.getRows(labels, this.collection.site, this.rowsSelected);
            if (!this.permissions) {
                //If we don't have permission remove the checkbox column from each row;
                for (var i in rows) {
                    rows[i].splice(0, 1);
                }
            }
            this.datatable.rows.add(rows);
            this.datatable.columns.adjust().draw(false);
        }
    }
});
