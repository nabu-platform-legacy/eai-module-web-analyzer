/*
* Copyright (C) 2017 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.module.web.analyzer;

import java.util.Date;
import java.util.List;

import be.nabu.glue.annotations.GlueParam;
import be.nabu.libs.evaluator.annotations.MethodProviderClass;
import be.nabu.libs.metrics.core.api.HistorySink;
import be.nabu.libs.metrics.core.api.ListableSinkProvider;
import be.nabu.libs.metrics.core.api.Sink;
import be.nabu.libs.metrics.core.api.SinkSnapshot;
import be.nabu.libs.metrics.core.api.SinkStatistics;
import be.nabu.libs.metrics.core.api.SinkValue;
import be.nabu.libs.metrics.core.api.StatisticsContainer;
import be.nabu.libs.metrics.core.api.TaggableSink;

@MethodProviderClass(namespace = "metrics")
public class MetricsMethods {
	
	private ListableSinkProvider provider;
	private String type;

	public MetricsMethods(ListableSinkProvider provider, String type) {
		this.provider = provider;
		this.type = type;
	}
	
	public List<SinkValue> until(String id, String category, int amount, Date until) {
		Sink sink = provider.getSink(id, category);
		SinkSnapshot snapshot = sink instanceof HistorySink ? ((HistorySink) sink).getSnapshotUntil(amount, until == null ? new Date().getTime() : until.getTime()) : null;
		return snapshot == null ? null : snapshot.getValues();
	}
	
	public List<SinkValue> between(String id, String category, Date from, Date until) {
		Sink sink = provider.getSink(id, category);
		SinkSnapshot snapshot = sink instanceof HistorySink ? ((HistorySink) sink).getSnapshotBetween(from.getTime(), until == null ? new Date().getTime() : until.getTime()) : null;
		return snapshot == null ? null : snapshot.getValues();
	}
	
	public void push(@GlueParam(name = "id") String id, @GlueParam(name = "category") String category, @GlueParam(name = "value") Long value, @GlueParam(name = "timestamp") Date timestamp) {
		Sink sink = provider.getSink(id, category);
		sink.push(timestamp == null ? new Date().getTime() : timestamp.getTime(), value == null ? 1l : value);
		if (sink instanceof TaggableSink) {
			String tag = ((TaggableSink) sink).getTag("type");
			if (tag == null) {
				((TaggableSink) sink).setTag("type", type);
			}
		}
	}
	
	public SinkStatistics statistics(String id, String category) {
		Sink sink = provider.getSink(id, category);
		return sink instanceof StatisticsContainer ? ((StatisticsContainer) sink).getStatistics() : null;
	}
	
}
