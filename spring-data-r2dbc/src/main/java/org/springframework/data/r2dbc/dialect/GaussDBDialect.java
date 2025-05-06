/*
 * Copyright 2019-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.r2dbc.dialect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.data.relational.core.dialect.ArrayColumns;
import org.springframework.data.relational.core.dialect.ObjectArrayColumns;
import org.springframework.data.util.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.util.ClassUtils;

import io.r2dbc.gaussdb.codec.Json;

/**
 * An SQL dialect for GaussDB.
 *
 * Notes: this file is token from PostgresDialect and add specific changes for GaussDB
 *
 * @author liubao
 */
public class GaussDBDialect extends org.springframework.data.relational.core.dialect.GaussDBDialect
		implements R2dbcDialect {

	private static final Set<Class<?>> SIMPLE_TYPES;

	private static final boolean JSON_PRESENT = ClassUtils.isPresent("io.r2dbc.gaussdb.codec.Json",
			GaussDBDialect.class.getClassLoader());

	private static final boolean GEO_TYPES_PRESENT = ClassUtils.isPresent("io.r2dbc.gaussdb.codec.Polygon",
			GaussDBDialect.class.getClassLoader());

	static {

		Set<Class<?>> simpleTypes = new HashSet<>(
				org.springframework.data.relational.core.dialect.GaussDBDialect.INSTANCE.simpleTypes());

		// conditional GaussDB Geo support.
		Stream.of("io.r2dbc.gaussdb.codec.Box", //
						"io.r2dbc.gaussdb.codec.Circle", //
						"io.r2dbc.gaussdb.codec.Line", //
						"io.r2dbc.gaussdb.codec.Lseg", //
						"io.r2dbc.gaussdb.codec.Point", //
						"io.r2dbc.gaussdb.codec.Path", //
						"io.r2dbc.gaussdb.codec.Polygon") //
				.forEach(s -> ifClassPresent(s, simpleTypes::add));

		// conditional GaussDB JSON support.
		ifClassPresent("io.r2dbc.gaussdb.codec.Json", simpleTypes::add);

		// conditional GaussDB Interval support
		ifClassPresent("io.r2dbc.gaussdb.codec.Interval", simpleTypes::add);

		SIMPLE_TYPES = simpleTypes;
	}

	/**
	 * Singleton instance.
	 */
	public static final GaussDBDialect INSTANCE = new GaussDBDialect();

	private static final BindMarkersFactory INDEXED = BindMarkersFactory.indexed("$", 1);

	private final Lazy<ArrayColumns> arrayColumns = Lazy
			.of(() -> new SimpleTypeArrayColumns(ObjectArrayColumns.INSTANCE, getSimpleTypeHolder()));

	@Override
	public BindMarkersFactory getBindMarkersFactory() {
		return INDEXED;
	}

	@Override
	public Collection<? extends Class<?>> getSimpleTypes() {
		return SIMPLE_TYPES;
	}

	@Override
	public ArrayColumns getArraySupport() {
		return this.arrayColumns.get();
	}

	@Override
	public Collection<Object> getConverters() {

		if (!GEO_TYPES_PRESENT && !JSON_PRESENT) {
			return Collections.emptyList();
		}

		List<Object> converters = new ArrayList<>();

		if (GEO_TYPES_PRESENT) {
			converters.addAll(Arrays.asList(org.springframework.data.r2dbc.dialect.GaussDBDialect.FromGaussDBPointConverter.INSTANCE,
					org.springframework.data.r2dbc.dialect.GaussDBDialect.ToGaussDBPointConverter.INSTANCE, //
					org.springframework.data.r2dbc.dialect.GaussDBDialect.FromGaussDBCircleConverter.INSTANCE,
					org.springframework.data.r2dbc.dialect.GaussDBDialect.ToGaussDBCircleConverter.INSTANCE, //
					org.springframework.data.r2dbc.dialect.GaussDBDialect.FromGaussDBBoxConverter.INSTANCE,
					org.springframework.data.r2dbc.dialect.GaussDBDialect.ToGaussDBBoxConverter.INSTANCE, //
					org.springframework.data.r2dbc.dialect.GaussDBDialect.FromGaussDBPolygonConverter.INSTANCE,
					org.springframework.data.r2dbc.dialect.GaussDBDialect.ToGaussDBPolygonConverter.INSTANCE));
		}

		if (JSON_PRESENT) {
			converters.addAll(Arrays.asList(JsonToByteArrayConverter.INSTANCE, JsonToStringConverter.INSTANCE));
		}

		return converters;
	}

	/**
	 * If the class is present on the class path, invoke the specified consumer {@code action} with the class object,
	 * otherwise do nothing.
	 *
	 * @param action block to be executed if a value is present.
	 */
	private static void ifClassPresent(String className, Consumer<Class<?>> action) {

		if (ClassUtils.isPresent(className, GaussDBDialect.class.getClassLoader())) {
			action.accept(ClassUtils.resolveClassName(className, GaussDBDialect.class.getClassLoader()));
		}
	}

	@ReadingConverter
	private enum FromGaussDBBoxConverter implements Converter<io.r2dbc.gaussdb.codec.Box, Box> {

		INSTANCE;

		@Override
		public Box convert(io.r2dbc.gaussdb.codec.Box source) {
			return new Box(org.springframework.data.r2dbc.dialect.GaussDBDialect.FromGaussDBPointConverter.INSTANCE.convert(source.getA()),
					org.springframework.data.r2dbc.dialect.GaussDBDialect.FromGaussDBPointConverter.INSTANCE.convert(source.getB()));
		}
	}

	@WritingConverter
	private enum ToGaussDBBoxConverter implements Converter<Box, io.r2dbc.gaussdb.codec.Box> {

		INSTANCE;

		@Override
		public io.r2dbc.gaussdb.codec.Box convert(Box source) {
			return io.r2dbc.gaussdb.codec.Box.of(
					org.springframework.data.r2dbc.dialect.GaussDBDialect.ToGaussDBPointConverter.INSTANCE.convert(source.getFirst()),
					org.springframework.data.r2dbc.dialect.GaussDBDialect.ToGaussDBPointConverter.INSTANCE.convert(source.getSecond()));
		}
	}

	@ReadingConverter
	private enum FromGaussDBCircleConverter implements Converter<io.r2dbc.gaussdb.codec.Circle, Circle> {

		INSTANCE;

		@Override
		public Circle convert(io.r2dbc.gaussdb.codec.Circle source) {
			return new Circle(source.getCenter().getX(), source.getCenter().getY(), source.getRadius());
		}
	}

	@WritingConverter
	private enum ToGaussDBCircleConverter implements Converter<Circle, io.r2dbc.gaussdb.codec.Circle> {

		INSTANCE;

		@Override
		public io.r2dbc.gaussdb.codec.Circle convert(Circle source) {
			return io.r2dbc.gaussdb.codec.Circle.of(source.getCenter().getX(), source.getCenter().getY(),
					source.getRadius().getValue());
		}
	}

	@ReadingConverter
	private enum FromGaussDBPolygonConverter implements Converter<io.r2dbc.gaussdb.codec.Polygon, Polygon> {

		INSTANCE;

		@Override
		public Polygon convert(io.r2dbc.gaussdb.codec.Polygon source) {

			List<io.r2dbc.gaussdb.codec.Point> sourcePoints = source.getPoints();
			List<Point> targetPoints = new ArrayList<>(sourcePoints.size());

			for (io.r2dbc.gaussdb.codec.Point sourcePoint : sourcePoints) {
				targetPoints.add(org.springframework.data.r2dbc.dialect.GaussDBDialect.FromGaussDBPointConverter.INSTANCE.convert(sourcePoint));
			}

			return new Polygon(targetPoints);
		}
	}

	@WritingConverter
	private enum ToGaussDBPolygonConverter implements Converter<Polygon, io.r2dbc.gaussdb.codec.Polygon> {

		INSTANCE;

		@Override
		public io.r2dbc.gaussdb.codec.Polygon convert(Polygon source) {

			List<Point> sourcePoints = source.getPoints();
			List<io.r2dbc.gaussdb.codec.Point> targetPoints = new ArrayList<>(sourcePoints.size());

			for (Point sourcePoint : sourcePoints) {
				targetPoints.add(org.springframework.data.r2dbc.dialect.GaussDBDialect.ToGaussDBPointConverter.INSTANCE.convert(sourcePoint));
			}

			return io.r2dbc.gaussdb.codec.Polygon.of(targetPoints);
		}
	}

	@ReadingConverter
	private enum FromGaussDBPointConverter implements Converter<io.r2dbc.gaussdb.codec.Point, Point> {

		INSTANCE;

		@Override
		@NonNull
		public Point convert(io.r2dbc.gaussdb.codec.Point source) {
			return new Point(source.getX(), source.getY());
		}
	}

	@WritingConverter
	private enum ToGaussDBPointConverter implements Converter<Point, io.r2dbc.gaussdb.codec.Point> {

		INSTANCE;

		@Override
		@NonNull
		public io.r2dbc.gaussdb.codec.Point convert(Point source) {
			return io.r2dbc.gaussdb.codec.Point.of(source.getX(), source.getY());
		}
	}

	@ReadingConverter
	private enum JsonToStringConverter implements Converter<Json, String> {

		INSTANCE;

		@Override
		@NonNull
		public String convert(Json source) {
			return source.asString();
		}
	}

	@ReadingConverter
	private enum JsonToByteArrayConverter implements Converter<Json, byte[]> {

		INSTANCE;

		@Override
		@NonNull
		public byte[] convert(Json source) {
			return source.asArray();
		}
	}
}
