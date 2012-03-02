import gettext
import logging
import mx
import reports.i18n_helper
import reports.engine
import reports.sql_helper as sql_helper

from mx.DateTime import DateTimeDelta
from psycopg2.extensions import DateFromMx
from psycopg2.extensions import QuotedString
from psycopg2.extensions import TimestampFromMx
from reports import Chart
from reports import ColumnDesc
from reports import DATE_FORMATTER
from reports import DetailSection
from reports import Graph
from reports import Highlight
from reports import KeyStatistic
from reports import PIE_CHART
from reports import Report
from reports import SummarySection
from reports import TIMESTAMP_FORMATTER
from reports import TIME_OF_DAY_FORMATTER
from reports import TIME_SERIES_CHART
from reports.engine import Column
from reports.engine import FactTable
from reports.engine import Node
from reports.engine import get_wan_clause
from reports.sql_helper import print_timing

EVT_TYPE_REGISTER = 0
EVT_TYPE_RENEW    = 1
EVT_TYPE_EXPIRE   = 2
EVT_TYPE_RELEASE  = 3

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext
def N_(message): return message

class UvmNode(Node):
    def __init__(self):
        Node.__init__(self, 'untangle-vm')

    @print_timing
    def setup(self, start_date, end_date, start_time):

        self.__do_housekeeping()

        self.__create_n_admin_logins(start_date, end_date, start_time)

        self.__make_sessions_table(start_date, end_date, start_time)

        ft = FactTable('reports.session_totals',
                       'reports.sessions',
                       'time_stamp',
                       [Column('hname', 'text'),
                        Column('uid', 'text'),
                        Column('policy_id', 'bigint'),
                        Column('client_intf', 'smallint'),
                        Column('server_intf', 'smallint'),
                        Column('c_server_port', 'int4'),
                        Column('c_client_port', 'int4')],
                       [Column('new_sessions', 'bigint', 'count(*)'),
                        Column('s2c_bytes', 'bigint', 'sum(p2c_bytes)'),
                        Column('c2s_bytes', 'bigint', 'sum(p2s_bytes)')])
        reports.engine.register_fact_table(ft)


        self.branded_name = self.__get_branded_name() or self.name

    @print_timing
    def __get_branded_name(self):
        conn = sql_helper.get_connection()
        curs = conn.cursor()
        try:
            curs.execute("SELECT company_name FROM n_branding_settings")
            while 1:
                r = curs.fetchone()
                if not r:
                    break
                return r[0]
        except Exception, e:
            return self.name
        finally:
            conn.commit()
        return None
        
    @print_timing
    def __create_n_admin_logins(self, start_date, end_date, start_time):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.n_admin_logins (
    time_stamp timestamp without time zone,
    login text,
    local boolean,
    client_addr inet,
    succeeded boolean,
    reason char(1) )""", 'time_stamp', start_date, end_date)

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.n_admin_logins
      (time_stamp, login, local, client_addr, succeeded, reason)
    SELECT time_stamp, login, local, client_addr, succeeded, reason
    FROM events.u_login_evt evt
    WHERE time_stamp < %s::timestamp without time zone""",
                               (start_time,), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def post_facttable_setup(self, start_date, end_date):
        self.__make_session_counts_table(start_date, end_date)
        self.__make_hnames_table(start_date, end_date)
        self.__make_users_table(start_date, end_date)

    @sql_helper.print_timing
    def events_cleanup(self, cutoff):
        sql_helper.clean_table("events", "u_login_evt", cutoff);

    def reports_cleanup(self, cutoff):
        sql_helper.drop_fact_table("n_admin_logins", cutoff)
        sql_helper.drop_fact_table("users", cutoff)
        sql_helper.drop_fact_table("hnames", cutoff)
        sql_helper.drop_fact_table("sessions", cutoff)
        sql_helper.drop_fact_table("session_totals", cutoff)        
        sql_helper.drop_fact_table("session_counts", cutoff)

    def get_report(self):
        sections = []

        s = SummarySection('summary', _('Summary Report'),
                           [VmHighlight(self.name, self.branded_name),
                            BandwidthUsage(),
                            SessionsPerMinute(),
                            DestinationPorts()])

        sections.append(s)
        sections.append(AdministrativeLoginsDetail())

        return Report(self, sections)

    @print_timing
    def __make_users_table(self, start_date, end_date):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.users (
        date date NOT NULL,
        username text NOT NULL,
        PRIMARY KEY (date, username));
""", 'date', start_date, end_date)

        sd = sql_helper.get_max_timestamp_with_interval('reports.users')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.users (date, username)
    SELECT DISTINCT date_trunc('day', trunc_time)::date AS day, uid
    FROM reports.session_totals
    WHERE trunc_time >= %s::timestamp without time zone
    AND NOT uid ISNULL""", (sd,), connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            print e
            conn.rollback()
            raise e

    @print_timing
    def __make_hnames_table(self, start_date, end_date):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.hnames (
        date date NOT NULL,
        hname text NOT NULL,
        PRIMARY KEY (date, hname));
""", 'date', start_date, end_date)

        sd = sql_helper.get_max_timestamp_with_interval('reports.hnames')

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.hnames (date, hname)
    SELECT DISTINCT date_trunc('day', trunc_time)::date, hname
    FROM reports.session_totals
    WHERE trunc_time >= %s::timestamp without time zone
    AND server_intf IN """ + get_wan_clause() + """
    AND client_intf NOT IN """ + get_wan_clause() + """
    AND NOT hname ISNULL""", (sd,),
                               connection=conn, auto_commit=False)
            conn.commit()
        except Exception, e:
            print e
            conn.rollback()
            raise e

    @print_timing
    def __make_sessions_table(self, start_date, end_date, start_time):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.sessions (
        session_id int8 NOT NULL,
        event_id bigserial,
        time_stamp timestamp NOT NULL,
        end_time timestamp NOT NULL,
        hname text,
        uid text,
        policy_id bigint,
        c_client_addr inet,
        c_server_addr inet,
        c_server_port int4,
        c_client_port int4,
        s_client_addr inet,
        s_server_addr inet,
        s_server_port int4,
        s_client_port int4,
        client_intf int2,
        server_intf int2,
        c2p_bytes int8,
        p2c_bytes int8,
        s2p_bytes int8,
        p2s_bytes int8)""", 'time_stamp', start_date, end_date)

        sql_helper.add_column('reports', 'sessions', 'event_id', 'bigserial')
        sql_helper.add_column('reports', 'sessions', 'policy_id', 'bigint')
        sql_helper.add_column('reports', 'sessions', 'server_intf', 'int2')
        sql_helper.add_column('reports', 'sessions', 'bandwidth_priority', 'bigint')
        sql_helper.add_column('reports', 'sessions', 'bandwidth_rule', 'bigint')
        sql_helper.add_column('reports', 'sessions', 'firewall_was_blocked', 'boolean')
        sql_helper.add_column('reports', 'sessions', 'firewall_rule_index', 'integer')
        sql_helper.add_column('reports', 'sessions', 'firewall_rule_description', 'text')
        sql_helper.add_column('reports', 'sessions', 'pf_protocol', 'text')
        sql_helper.add_column('reports', 'sessions', 'pf_blocked', 'boolean')
        sql_helper.add_column('reports', 'sessions', 'classd_application', 'text')
        sql_helper.add_column('reports', 'sessions', 'classd_protochain', 'text')
        sql_helper.add_column('reports', 'sessions', 'classd_blocked', 'boolean')
        sql_helper.add_column('reports', 'sessions', 'classd_flagged', 'boolean')
        sql_helper.add_column('reports', 'sessions', 'classd_confidence', 'integer')
        sql_helper.add_column('reports', 'sessions', 'classd_ruleid', 'integer')
        sql_helper.add_column('reports', 'sessions', 'classd_detail', 'text')
        sql_helper.add_column('reports', 'sessions', 'ips_blocked', 'boolean')
        sql_helper.add_column('reports', 'sessions', 'ips_name', 'text')
        sql_helper.add_column('reports', 'sessions', 'ips_description', 'text')
        sql_helper.add_column('reports', 'sessions', 'sw_access_ident', 'text')
        sql_helper.add_column('reports', 'sessions', 's_client_addr', 'inet')
        sql_helper.add_column('reports', 'sessions', 's_server_addr', 'inet')
        sql_helper.add_column('reports', 'sessions', 's_server_port', 'int4')
        sql_helper.add_column('reports', 'sessions', 's_client_port', 'int4')

        # we used to create event_id as serial instead of bigserial - convert if necessary
        sql_helper.convert_column("reports","sessions","event_id","integer","bigint");

        # rename pl_endp_id to session_id
        sql_helper.rename_column("reports","sessions","pl_endp_id","session_id");
        sql_helper.rename_index("reports","sessions_pl_endp_id_idx","sessions_session_id_idx");

        # If the new index does not exist, create it
        if not sql_helper.index_exists("reports","sessions","session_id", unique=True):
            sql_helper.create_index("reports","sessions","session_id", unique=True);
        # If the new index does exist, delete the old one
        if sql_helper.index_exists("reports","sessions","session_id", unique=True):
            sql_helper.drop_index("reports","sessions","session_id", unique=False);

        # If the new index does not exist, create it
        if not sql_helper.index_exists("reports","sessions","event_id", unique=True):
            sql_helper.create_index("reports","sessions","event_id", unique=True);
        # If the new index does exist, delete the old one
        if sql_helper.index_exists("reports","sessions","event_id", unique=True):
            sql_helper.drop_index("reports","sessions","event_id", unique=False);

        sql_helper.create_index("reports","sessions","policy_id");
        sql_helper.create_index("reports","sessions","time_stamp");

        # firewall event log query indexes
        # sql_helper.create_index("reports","sessions","firewall_rule_index");
        # sql_helper.create_index("reports","sessions","firewall_was_blocked");

        # spyware event log query indexes
        # sql_helper.create_index("reports","sessions","sw_access_ident");

        # ips event log query indexes
        # sql_helper.create_index("reports","sessions","ips_name");
        # sql_helper.create_index("reports","sessions","ips_blocked");

        # protofilter event log query indexes
        # sql_helper.create_index("reports","sessions","pf_protocol");
        # sql_helper.create_index("reports","sessions","pf_blocked");

        # bandwidth event log query indexes
        # sql_helper.create_index("reports","sessions","bandwidth_priority");

    @print_timing
    def __make_session_counts_table(self, start_date, end_date):
        sql_helper.create_fact_table("""\
CREATE TABLE reports.session_counts (
        trunc_time timestamp,
        uid text,
        hname text,
        client_intf smallint,
        server_intf smallint,
        num_sessions int8)""", 'trunc_time', start_date, end_date)

        sql_helper.add_column('reports', 'session_counts', 'client_intf', 'smallint')
        sql_helper.add_column('reports', 'session_counts', 'server_intf', 'smallint')

        sql_helper.create_index("reports","session_counts","trunc_time");

        sd = TimestampFromMx(sql_helper.get_update_info('reports.session_counts',
                                                        start_date))

        conn = sql_helper.get_connection()
        try:
            sql_helper.run_sql("""\
INSERT INTO reports.session_counts
    (trunc_time, uid, hname, client_intf, server_intf, num_sessions)
SELECT (date_trunc('minute', time_stamp)
        + (generate_series(0, (extract('epoch' from (end_time - time_stamp))
        / 60)::int) || ' minutes')::interval) AS time, uid, hname,
        client_intf, server_intf, count(*)
FROM reports.sessions
WHERE time_stamp >= %s::timestamp without time zone
GROUP BY time, uid, hname, client_intf, server_intf
""", (sd,), connection=conn, auto_commit=False)

            sql_helper.set_update_info('reports.session_counts', 
                                       TimestampFromMx(mx.DateTime.now()),
                                       connection=conn, auto_commit=False,
                                       origin_table='reports.sessions')
            conn.commit()
        except Exception, e:
            conn.rollback()
            raise e

    def teardown(self):
        pass

    @print_timing
    def __do_housekeeping(self):
        sql_helper.run_sql("""\
DELETE FROM settings.n_reporting_settings WHERE tid NOT IN
(SELECT tid FROM settings.u_node_persistent_state
WHERE NOT target_state = 'destroyed')""")

        sql_helper.run_sql("""\
DELETE FROM settings.u_ipmaddr_dir_entries WHERE ipmaddr_dir_id NOT IN
(SELECT id FROM settings.u_ipmaddr_dir WHERE id IN
(SELECT network_directory FROM settings.n_reporting_settings))""")

        sql_helper.run_sql("""\
DELETE FROM settings.u_ipmaddr_dir WHERE id NOT IN
(SELECT network_directory FROM settings.n_reporting_settings)""")

        if sql_helper.table_exists('reports', 'merged_address_map'):
            sql_helper.run_sql("DROP TABLE reports.merged_address_map");

class VmHighlight(Highlight):
    def __init__(self, name, branded_name):
        xml_escapes = { '&' : '&amp;',
                        '>' : '&gt;',
                        '<' : '&lt;',
                        "'" : '&apos',
                        '"' : '&quot;' }

        for char, escape in xml_escapes.iteritems():
            branded_name = branded_name.replace(char, escape)

        Highlight.__init__(self, name,
                           branded_name + " " +
                           _("scanned") + " " +
                           "%(traffic)s" + " " +
                           _("GB") +
                           " " + _("and") + " " +
                           "%(sessions)s" + " " + _("sessions"))

    @print_timing
    def get_highlights(self, end_date, report_days,
                       host=None, user=None, email=None):
        if host or user or email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """
SELECT (SELECT round((COALESCE(sum(s2c_bytes + c2s_bytes), 0) / 1000000000)::numeric, 2)
        FROM reports.session_totals
        WHERE trunc_time >= %s::timestamp without time zone AND trunc_time < %s::timestamp without time zone) AS traffic,
       (SELECT COALESCE(sum(num_sessions), 0)::int
        FROM reports.session_counts
        WHERE trunc_time >= %s::timestamp without time zone AND trunc_time < %s::timestamp without time zone) AS sessions"""

        conn = sql_helper.get_connection()
        curs = conn.cursor()

        h = {}
        try:
            curs.execute(query, (one_week, ed, one_week, ed))

            h = sql_helper.get_result_dictionary(curs)
                
        finally:
            conn.commit()

        return h

class BandwidthUsage(Graph):
    def __init__(self):
        Graph.__init__(self, 'bandwidth-usage', _('Bandwidth Usage'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        lks = []

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            # kB
            sums = ["coalesce(sum(s2c_bytes + c2s_bytes), 0) / 1000",]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            time_interval = 60
            q, h = sql_helper.get_averaged_query(sums, "reports.session_totals",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where,
                                                 time_interval = time_interval)
            curs.execute(q, h)

            dates = []
            throughput = []

            for r in curs.fetchall():
                dates.append(r[0])
                throughput.append(float(r[1]) / time_interval)

            if not throughput:
                throughput = [0,]

            ks = KeyStatistic(_('Avg Data Rate'),
                              sum(throughput)/len(throughput),
                              N_('kB/s'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Data Rate'), max(throughput), N_('kB/s'))
            lks.append(ks)
            ks = KeyStatistic(_('Data Transferred'), sum(throughput) * time_interval, N_('kB'))
            lks.append(ks)

                
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Date'), ylabel=_('Throughput (kB/s)'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, throughput, _('Usage'))

        return (lks, plot)

class SessionsPerMinute(Graph):
    def __init__(self):
        Graph.__init__(self, 'sessions-per-minute', _('Sessions per Minute'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        lks = []

        conn = sql_helper.get_connection()
        try:
            # per minute
            sums = ["coalesce(sum(num_sessions), 0)"]

            extra_where = []
            if host:
                extra_where.append(("hname = %(host)s", { 'host' : host }))
            elif user:
                extra_where.append(("uid = %(user)s" , { 'user' : user }))

            q, h = sql_helper.get_averaged_query(sums, "reports.session_counts",
                                                 end_date - mx.DateTime.DateTimeDelta(report_days),
                                                 end_date,
                                                 extra_where = extra_where)

            curs = conn.cursor()
            
            curs.execute(q, h)

            dates = []
            num_sessions = []

            for r in curs.fetchall():
                dates.append(r[0])
                num_sessions.append(float(r[1]))

            if not num_sessions:
                num_sessions = [0,]

            ks = KeyStatistic(_('Avg Sessions/Min'),
                              int(sum(num_sessions)/len(num_sessions)),
                              _('Sessions'))
            lks.append(ks)
            ks = KeyStatistic(_('Max Sessions/Min'),
                              int(max(num_sessions)), _('Sessions'))
            lks.append(ks)
            ks = KeyStatistic(_('Total Sessions'), int(sum(num_sessions)), _('sessions'))
            lks.append(ks)
        finally:
            conn.commit()

        plot = Chart(type=TIME_SERIES_CHART, title=self.title,
                     xlabel=_('Date'), ylabel=_('Sessions'),
                     major_formatter=TIMESTAMP_FORMATTER)

        plot.add_dataset(dates, num_sessions, _("Usage"))

        return (lks, plot)

class DestinationPorts(Graph):
    def __init__(self):
        Graph.__init__(self, 'top-dest-ports', _('Top Destination Ports'))

    @print_timing
    def get_graph(self, end_date, report_days, host=None, user=None,
                  email=None):
        if email:
            return None

        ed = DateFromMx(end_date)
        one_week = DateFromMx(end_date - mx.DateTime.DateTimeDelta(report_days))

        query = """\
SELECT c_server_port, sum(new_sessions)::int as sessions
FROM reports.session_totals
WHERE trunc_time >= %s::timestamp without time zone AND trunc_time < %s::timestamp without time zone"""

        if host:
            query += " AND hname = %s"
        elif user:
            query += " AND uid = %s"

        query += """
GROUP BY c_server_port
ORDER BY sessions DESC"""

        conn = sql_helper.get_connection()
        try:
            curs = conn.cursor()

            if host:
                curs.execute(query, (one_week, ed, host))
            elif user:
                curs.execute(query, (one_week, ed, user))
            else:
                curs.execute(query, (one_week, ed))


            lks = []
            pds = {}

            for r in curs.fetchall():
                port = r[0]
                sessions = r[1]
                ks = KeyStatistic(str(port), sessions, _('Sessions'))
                lks.append(ks)
                pds[port] = sessions
        finally:
            conn.commit()

        plot = Chart(type=PIE_CHART, title=self.title, xlabel=_('Port'),
                     ylabel=_('Sessions'))

        plot.add_pie_dataset(pds, display_limit=10)

        return (lks, plot, 10)

class Lease:
    def __init__(self, row):
        self.start = row[0]
        self.end_of_lease = row[1]
        self.ip = row[2]
        self.hostname = row[3]
        self.event_type = row[4]

    def after(self, event):
        return self.start > event.end_of_lease

    def intersects_before(self, event):
        return ((self.start > event.start
                 and self.start < event.end_of_lease)
                and (self.end_of_lease > event.end_of_lease
                     or self.end_of_lease == event.end_of_lease))

    def intersects_after(self, event):
        return ((self.start < event.start
                 and self.end_of_lease > event.start)
                and (self.end_of_lease == event.end_of_lease
                     or self.end_of_lease < event.end_of_lease))

    def encompass(self, event):
        return ((self.start == event.start or self.start < event.start)
                and (self.end_of_lease == event.end_of_lease
                     or self.end_of_lease > event.end_of_lease))

    def encompassed(self, event):
        return ((self.start == event.start or self.start > event.start)
                and ( self.end_of_lease == event.end_of_lease
                      or self.end_of_lease < event.end_of_lease))

    def values(self):
        return (self.ip, self.hostname,
                TimestampFromMx(sql_helper.date_convert(self.start)),
                TimestampFromMx(sql_helper.date_convert(self.end_of_lease)))

class AdministrativeLoginsDetail(DetailSection):
    def __init__(self):
        DetailSection.__init__(self, 'admin-logins-events', _('Administrative Logins Events'))

    def get_columns(self, host=None, user=None, email=None):
        if host or user or email:
            return None

        rv = [ColumnDesc('time_stamp', _('Time'), 'Date')]

        rv += [ColumnDesc('client_addr', _('Client Ip')),
               ColumnDesc('succeeded', _('Success'))]

        return rv

    def get_sql(self, start_date, end_date, host=None, user=None, email=None):
        if email:
            return None

        sql = """\
SELECT time_stamp, host(client_addr), succeeded::text
FROM reports.n_admin_logins
WHERE time_stamp >= %s::timestamp without time zone AND time_stamp < %s::timestamp without time zone AND not local
""" % (DateFromMx(start_date), DateFromMx(end_date))

        return sql + " ORDER BY time_stamp DESC"

reports.engine.register_node(UvmNode())
