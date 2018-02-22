Ext.define('Ung.view.reports.GraphData', {
    extend: 'Ext.Panel',
    alias: 'widget.graphdata',
    // title: 'Graph Data'.t(),
    viewModel: true,

    border: false,

    layout: 'fit',

    items: [{
        xtype: 'grid',
        cls: 'x-grid-events',
        emptyText: 'No Data!'.t(),
        striped: true,
        store: {
            data: []
        },
        columns: []
    }],

    controller: {
        control: {
            '#': {
                painted: 'onPainted',
                // deactivate: 'onDeactivate'
            }
        },

        viewModel: true,

        onPainted: function (view) {
            var me = this, vm = me.getViewModel(), grid = view.down('grid');
            vm.bind('{entry}', function (entry) {
                console.log(entry);
                grid.getStore().loadData([]);
                grid.setColumns([]);
            });

            // vm.bind('{fetching}', function (value) {
            //     me.getView().setLoading(value);
            // });
            vm.bind('{reportData}', function (data) {
                switch (vm.get('entry.type')) {
                case 'TEXT':               me.formatTextData(data); break;
                case 'PIE_GRAPH':          me.formatPieData(data); break;
                case 'TIME_GRAPH':         me.formatTimeData(data); break;
                case 'TIME_GRAPH_DYNAMIC': me.formatTimeDynamicData(data); break;
                }
            });
        },

        formatTextData: function (data) {
            var vm = this.getViewModel(),
                entry = vm.get('eEntry') || vm.get('entry'), i, column;

            this.getView().setColumns([{
                dataIndex: 'data',
                header: 'data'.t(),
                flex: 1
            }, {
                dataIndex: 'value',
                header: 'value'.t(),
                width: 200
            }]);

            var reportData = [], value;
            if (data.length > 0 && entry.get('textColumns') !== null) {
                for (i = 0; i < entry.get('textColumns').length; i += 1) {
                    column = entry.get('textColumns')[i].split(' ').splice(-1)[0];
                    value = Ext.isEmpty(data[0][column]) ? 0 : data[0][column];
                    reportData.push({data: column, value: value});
                }
            }
            // vm.set('_currentData', reportData);
            this.getView().getStore().loadData(reportData);
        },

        formatTimeData: function (data) {
            var me = this, vm = this.getViewModel(),
                grid = me.getView().down('grid'),
                entry = vm.get('eEntry') || vm.get('entry'), i, column, title;

            var reportDataColumns = [{
                dataIndex: 'time_trunc',
                text: 'Timestamp'.t(),
                width: 200,
                // flex: 1,
                renderer: Renderer.timestamp,
                sortable: true
            }];
            var reportDataFields = [
                { name: 'time_trunc', sortType: 'asTimestamp' }
            ];

            for (i = 0; i < entry.get('timeDataColumns').length; i += 1) {
                column = entry.get('timeDataColumns')[i].split(' ').splice(-1)[0];
                title = column;
                reportDataColumns.push({
                    dataIndex: column,
                    text: title,
                    width: 120,
                    renderer: function (val) {
                        return val !== undefined ? val : '-';
                    },
                    sortable: true
                });
                reportDataFields.push({
                    name: column, sortType: 'asFloat'
                });
            }

            console.log(reportDataColumns);
            grid.setColumns(reportDataColumns);
            grid.refresh();
            // grid.getStore().setFields(reportDataFields);
            grid.getStore().loadData(data);
        },

        formatTimeDynamicData: function (data) {
            var vm = this.getViewModel(),
                entry = vm.get('entry'),
                timeDataColumns = [], i, column;


            for (i = 0; i < data.length; i += 1) {
                for (var _column in data[i]) {
                    if (data[i].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && timeDataColumns.indexOf(_column) < 0) {
                        timeDataColumns.push(_column);
                    }
                }
            }

            var reportDataColumns = [{
                dataIndex: 'time_trunc',
                header: 'Timestamp'.t(),
                width: 130,
                flex: 1,
                renderer: function (val) {
                    return (!val) ? 0 : Util.timestampFormat(val);
                }
            }];
            var seriesRenderer = null, title;
            if (!Ext.isEmpty(entry.get('seriesRenderer'))) {
                seriesRenderer = Renderer[entry.get('seriesRenderer')];
            }

            for (i = 0; i < timeDataColumns.length; i += 1) {
                column = timeDataColumns[i];
                title = seriesRenderer ? seriesRenderer(column) + ' [' + column + ']' : column;
                // storeFields.push({name: timeDataColumns[i], type: 'integer'});
                reportDataColumns.push({
                    dataIndex: column,
                    header: title,
                    width: timeDataColumns.length > 2 ? 60 : 90
                });
            }

            this.getView().setColumns(reportDataColumns);
            this.getView().getStore().loadData(data);
            // vm.set('_currentData', data);
        },

        formatPieData: function (data) {
            var me = this, vm = me.getViewModel(),
                grid = me.getView().down('grid'),
                entry = vm.get('eEntry') || vm.get('entry');

            var header = '<strong>' + TableConfig.getColumnHumanReadableName(entry.get('pieGroupColumn')) + '</strong> <span style="float: right;">[' + entry.get('pieGroupColumn') + ']</span>';

            grid.setColumns([{
                dataIndex: entry.get('pieGroupColumn'),
                text: header,
                width: 150,
                renderer: Renderer[entry.get('pieGroupColumn')] || null
            }, {
                dataIndex: 'value',
                text: 'value'.t(),
                width: 200,
                renderer: function (value) {
                    if (entry.get('units') === 'bytes' || entry.get('units') === 'bytes/s') {
                        return Util.bytesToHumanReadable(value, true);
                    } else {
                        return value;
                    }
                }
            }, {
                // xtype: 'actioncolumn',
                // menuDisabled: true,
                // width: 30,
                // align: 'center',
                // items: [{
                //     iconCls: 'fa fa-filter',
                //     tooltip: 'Add Condition'.t(),
                //     handler: 'addPieFilter'
                // }]
            }]);
            grid.getStore().loadData(data);
        },

        addPieFilter: function (view, rowIndex, colIndex, item, e, record) {
            var me = this, vm = me.getViewModel(),
                // gridFilters =  me.getView().down('#sqlFilters'),
                col = vm.get('entry.pieGroupColumn');

            if (col) {
                me.getView().up('entry').down('globalconditions').getStore().add({
                    column: col,
                    operator: '=',
                    value: record.get(col),
                    javaClass: 'com.untangle.app.reports.SqlCondition'
                });
            } else {
                console.log('Issue with pie column!');
                return;
            }
        },

        /**
         * exports graph data
         */
        exportGraphData: function (btn) {
            var me = this, vm = me.getViewModel(), entry = vm.get('entry').getData(), columns = [], headers = [], j;
            if (!entry) { return; }

            var grid = btn.up('grid'), csv = [];

            if (!grid) {
                console.log('Grid not found');
                return;
            }

            var processRow = function (row) {
                var data = [], j, innerValue;
                for (j = 0; j < row.length; j += 1) {
                    innerValue = !row[j] ? '' : row[j].toString();
                    data.push('"' + innerValue.replace(/"/g, '""') + '"');
                }
                return data.join(',') + '\r\n';
            };

            Ext.Array.each(grid.getColumns(), function (col) {
                if (col.dataIndex && !col.hidden) {
                    columns.push(col.dataIndex);
                    headers.push(col.text);
                }
            });
            csv.push(processRow(headers));

            grid.getStore().each(function (row) {
                var r = [];
                for (j = 0; j < columns.length; j += 1) {
                    if (columns[j] === 'time_trunc') {
                        r.push(Util.timestampFormat(row.get('time_trunc')));
                    } else {
                        r.push(row.get(columns[j]));
                    }
                }
                csv.push(processRow(r));
            });

            me.download(csv.join(''), (entry.category + '-' + entry.title + '-' + Ext.Date.format(new Date(), 'd.m.Y-Hi')).replace(/ /g, '_') + '.csv', 'text/csv');
        },

        download: function(content, fileName, mimeType) {
            var a = document.createElement('a');
            mimeType = mimeType || 'application/octet-stream';

            if (navigator.msSaveBlob) { // IE10
                return navigator.msSaveBlob(new Blob([ content ], {
                    type : mimeType
                }), fileName);
            } else if ('download' in a) { // html5 A[download]
                a.href = 'data:' + mimeType + ',' + encodeURIComponent(content);
                a.setAttribute('download', fileName);
                document.body.appendChild(a);
                setTimeout(function() {
                    a.click();
                    document.body.removeChild(a);
                }, 100);
                return true;
            } else { //do iframe dataURL download (old ch+FF):
                var f = document.createElement('iframe');
                document.body.appendChild(f);
                f.src = 'data:' + mimeType + ',' + encodeURIComponent(content);
                setTimeout(function() {
                    document.body.removeChild(f);
                }, 400);
                return true;
            }
        }

    }

});
