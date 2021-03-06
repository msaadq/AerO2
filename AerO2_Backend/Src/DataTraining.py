import Maps as mp
import DataBaseLayer as dbl


class DataTraining:
    """
    Data Training class contains methods for Populating the Properties Table according to data provided by
    DataBaseLayer and Maps. It also has methods for Populating the Results Table based on the results provided
    by the ML algorithm
    """

    DEFAULT_NODE_LENGTH = 100.0
    VERIFICATION_THRESHOLD = 0.8
    INITIALIZE_FROM = 0

    _long_interval = 0.0
    _lat_interval = 0.0

    _city_name = None

    _maps = None
    _database = None

    def __init__(self):
        """
        Initializes the database and Maps API
        """

        self._maps = mp.Maps()
        self._data_base = dbl.DataBaseLayer()
        pass

    def initialize(self, city_name):
        """
        Deletes any previous entries for a city and populates fresh data in the Properties Table with data for a
        particular city.

        :param city_name: City name (String)

        :return no_of_modifications: (Int)
        """

        print "Started"

        self._city_name = city_name.lower()

        if self.INITIALIZE_FROM == 0:
            self._data_base.delete_data(self._data_base.PROP_TABLE_NAME,self._data_base.key_value_string_gen('city', self._city_name))

        return self._save_all_properties(city_name)

    def _save_all_properties(self, city_name):
        """
        Populates properties data in the Properties Table with data for a
        particular city after appending all the values.

        :param city_name: City name (String)

        :return no_of_modifications: (Int)
        """

        print "Saving Node Properties"
        count = self.INITIALIZE_FROM + 1

        final_table = []
        n_saved = 0
        self._maps.save_industries(city_name)

        all_coordinates = self._get_all_coordinates(self._maps.get_corner_coordinates(city_name))
        self._maps.set_intervals(self._lat_interval, self._long_interval)

        all_coordinates = all_coordinates[self.INITIALIZE_FROM:]

        t_len = len(all_coordinates)
        for i in range(self.INITIALIZE_FROM, t_len, 1000):
            if (t_len - i) > 1000:
                max_index = 1000
            else:
                max_index = t_len - i

            for j in range(i, i+max_index):
                print "Saved Node: " + str(count) + " Coordinates: ", all_coordinates[j][0], all_coordinates[j][1]
                count = count + 1
                final_table.append([0, all_coordinates[j][0], all_coordinates[j][1], self._maps.get_altitude(all_coordinates[j]), 0,
                                self._maps.get_road_index(all_coordinates[j]), self._maps.get_industry_index(all_coordinates[j])])

            n_saved += self._data_base.insert_multiple(self._data_base.PROP_TABLE_NAME, final_table[i - self.INITIALIZE_FROM:i - self.INITIALIZE_FROM + max_index],
                                                       self._city_name)

        return n_saved

    def _get_all_coordinates(self, corner_coordinates):
        """
        Calculates all the node coordinates of a city, separated by 50m using its corner coordinates

        :param corner_coordinates:

        :return: all_coordinates:
        """

        nodes_coordinates = []
        if corner_coordinates == [[]]:
            return [[]]

        y1, y2, x1, x2 = corner_coordinates[0][0], corner_coordinates[2][0], corner_coordinates[0][1], \
                         corner_coordinates[2][1]
        if x1 > x2:
            x1, x2 = x2, x1
        if y1 > y2:
            y1, y2 = y2, y1

        self._long_interval = (x2 - x1) / (
        self._maps.calc_distance_on_unit_sphere(y1, x1, y1, x2) / self.DEFAULT_NODE_LENGTH)
        self._lat_interval = (y2 - y1) / (
        self._maps.calc_distance_on_unit_sphere(y1, x1, y2, x1) / self.DEFAULT_NODE_LENGTH)

        latitude = y2
        while latitude > y1:
            longitude = x1

            while longitude < x2:
                nodes_coordinates.append([latitude, longitude])
                longitude += self._long_interval

            latitude -= self._lat_interval

        return nodes_coordinates

    def update_database(self):
        if self._normalize_all() > 0:
            if self._train_system() > self.VERIFICATION_THRESHOLD:
                return self._update_results()

        return -1

    def _normalize_all(self):
        not_normalized_data_table = self._database.select_data(self._database.SAMPLE_TABLE_NAME,
                                                               self._database.key_value_string_gen("normalized", 0))
        normalized_data_table = []
        temp = []
        average = 0.0

        for data_row in not_normalized_data_table:
            temp.append(self._database.select_data(self._database.SAMPLE_TABLE_NAME,
                                                   self._database.nearby_string_gen(data_row[0], data_row[1], 10)))

            for data in temp:
                average += data[self.DEFAULT_SMOG_INDEX]

            average /= len(temp)

            insert_row = [temp[0][DEFAULT_LAT_INDEX], temp[0][DEFAULT_LONG_INDEX], temp[0][DEFAULT_ALT_INDEX], average,
                          1]

            self._database.insert_row(self._database.SAMPLE_TABLE_NAME, insert_row)

    def _train_system(self):
        pass

    def _update_results(self):
        pass
